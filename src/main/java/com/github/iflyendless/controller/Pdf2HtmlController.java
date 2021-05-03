package com.github.iflyendless.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ZipUtil;
import com.github.iflyendless.config.Pdf2HtmlProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileFilter;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

@Slf4j
@RestController
@RequestMapping("/api")
public class Pdf2HtmlController {

    private static final String PDF = "pdf";
    private static final String FAILED_PDF_DIR = "failed-pdfs";
    private static final String TASK_FILE = "000-task.txt";

    @Resource
    private Pdf2HtmlProperties pdf2HtmlProperties;

    // 为了限制同时启动pdf2htmlEX命令行工具的子进程数
    private static Semaphore semaphore;

    // 转换html失败的pdf写到这个目录, 方便后面手动转换排查原因
    private static File failedPdfDir;

    @PostConstruct
    public void init() {
        semaphore = new Semaphore(pdf2HtmlProperties.getMaxProcess());
        failedPdfDir = FileUtil.mkdir(FileUtil.file(pdf2HtmlProperties.getWorkDir(), FAILED_PDF_DIR));
    }

    @GetMapping("/version")
    public Object version() {
        return "1.0.1";
    }

    @GetMapping("/config")
    public Object config() {
        return pdf2HtmlProperties;
    }

    @GetMapping("/metric")
    public Object metric() {
        Map<String, Object> semaphoreMap = new LinkedHashMap<>();
        semaphoreMap.put("availablePermits", semaphore.availablePermits());
        semaphoreMap.put("queueLength", semaphore.getQueueLength());

        Map<String, Object> metricMap = new LinkedHashMap<>();
        metricMap.put("semaphore", semaphoreMap);

        return metricMap;
    }

    @PostMapping("/pdf2html")
    public void pdf2html(@RequestParam("files") MultipartFile[] files,
                         HttpServletResponse response) {
        if (ArrayUtil.isEmpty(files)) {
            log.warn("文件数为0");
            return;
        }

        File dir = FileUtil.mkdir(FileUtil.file(pdf2HtmlProperties.getWorkDir(), IdUtil.simpleUUID()));

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            List<File> fileList = new ArrayList<>(files.length);
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) {
                    continue;
                }
                // 写入本地工作目录
                File localFile = FileUtil.writeFromStream(f.getInputStream(), FileUtil.file(dir, f.getOriginalFilename()));
                // 只处理pdf文件
                if (isPdf(localFile)) {
                    fileList.add(localFile);
                }
            }

            if (CollUtil.isEmpty(fileList)) {
                return;
            }

            long start = System.currentTimeMillis();

            int size = fileList.size();
            CountDownLatch latch = new CountDownLatch(size);
            // 处理失败的pdf统计
            Map<String, Throwable> failedMap = new ConcurrentHashMap<>();

            for (File file : fileList) {
                // 这里限制启动子进程的数量
                // 因为后面的调用是异步的, 防止瞬间产生大量子进程
                semaphore.acquire();
                // 异步调用pdf2htmlEX命令行工具
                invokeCommand(dir, file, latch, failedMap);
            }

            // 等待所有子进程结束
            latch.await();

            log.info("pdf2html一共耗时{}ms, pdf数量为{}", System.currentTimeMillis() - start, size);

            // 记录 统计数据写入文件000-task.txt, 转换html失败的pdf写入固定目录
            recordTaskResult(size, failedMap, dir, fileList);

            // 将生成的html文件以及task.txt压缩, 并写入response
            ZipUtil.zip(outputStream, CharsetUtil.CHARSET_UTF_8, true, new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.isDirectory()) {
                        return true;
                    }
                    String name = pathname.getName().toLowerCase();
                    return name.endsWith(".html") || name.endsWith(".txt");
                }
            }, dir);

            response.addHeader("Content-Disposition",
                    "attachment;fileName=" + URLEncoder.encode(dir.getName() + ".zip", "UTF-8"));
            response.addHeader("Content-type", "application/zip");
        } catch (Throwable e) {
            log.error("pdf2html error", e);
        } finally {
            FileUtil.del(dir);
        }
    }

    /**
     * 这里使用apache的commons-exec执行pdf2htmlEX命令行工具
     * 详情见: https://commons.apache.org/proper/commons-exec/tutorial.html
     */
    public void invokeCommand(File workDir, File file, CountDownLatch latch, Map<String, Throwable> failedMap) {
        String filePath = file.getAbsolutePath();

        String line = String.format("%s --dest-dir %s %s", pdf2HtmlProperties.getCommand(), workDir.getAbsolutePath(), filePath);
        CommandLine commandLine = CommandLine.parse(line);

        // 命令行的超时处理
        ExecuteWatchdog watchdog = new ExecuteWatchdog(1000 * pdf2HtmlProperties.getCommandTimeout().getSeconds());
        // 命令行 执行完成的回调
        ResultHandler resultHandler = new ResultHandler(file, latch, failedMap);

        Executor executor = new DefaultExecutor();
        executor.setExitValue(0);
        executor.setWatchdog(watchdog);

        try {
            executor.execute(commandLine, resultHandler);
        } catch (Throwable e) {
            semaphore.release();
            String fileName = file.getName();
            if (!failedMap.containsKey(fileName)) {
                failedMap.put(fileName, e);
            }
            latch.countDown();

            log.error("invokeCommand failed, command: {}, error:{}", line, e);
        }
    }

    public static boolean isPdf(File file) {
        try {
            return PDF.equalsIgnoreCase(FileTypeUtil.getType(file));
        } catch (Exception e) {
            log.error("识别pdf类型失败, 文件名:{}, error: {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    public static void recordTaskResult(int total, Map<String, Throwable> failedMap, File workDir, List<File> pdfs) {
        List<String> list = new ArrayList<>();
        list.add("total:" + total);
        list.add("success:" + (total - failedMap.size()));
        list.add("failed:" + failedMap.size());

        list.add("");
        list.add("failed-pdfs:");
        list.add("");

        Set<String> failedNames = failedMap.keySet();
        list.addAll(failedNames);

        // 记录任务完成大致情况
        FileUtil.writeLines(list, FileUtil.file(workDir, TASK_FILE), CharsetUtil.CHARSET_UTF_8);

        // 转换失败的pdf写入其他目录,后续可能需要进一步处理
        if (CollUtil.isNotEmpty(failedNames)) {
            for (File pdf : pdfs) {
                String name = pdf.getName();
                if (failedNames.contains(name)) {
                    File dest = FileUtil.file(failedPdfDir, name);
                    if (dest.exists()) {
                        dest = FileUtil.file(failedPdfDir, IdUtil.simpleUUID() + "-" + name);
                    }
                    FileUtil.copyFile(pdf, dest);
                }
            }
        }
    }

    /**
     * 根据具体的业务逻辑做相应的实现, 这里会打印一下错误日志
     */
    public static class ResultHandler implements ExecuteResultHandler {

        private final File file;
        private final CountDownLatch latch;
        private final Map<String, Throwable> failedMap;

        @Getter
        private int exitValue = -8686;

        public ResultHandler(File file, CountDownLatch latch, Map<String, Throwable> failedMap) {
            this.file = file;
            this.latch = latch;
            this.failedMap = failedMap;
        }

        @Override
        public void onProcessComplete(int exitValue) {
            semaphore.release();
            this.latch.countDown();

            this.exitValue = exitValue;
        }

        @Override
        public void onProcessFailed(ExecuteException e) {
            semaphore.release();
            this.failedMap.put(this.file.getName(), e);
            this.latch.countDown();

            log.error("pdf2html failed, file: {}, error:{}", this.file.getAbsolutePath(), e);
        }
    }
}
