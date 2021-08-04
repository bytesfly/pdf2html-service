
Convert PDF to HTML without losing text or format.

用`springboot`把`pdf2htmlEX`命令行工具包装为`web`服务, 使得`PDF`转`HTML`更方便。 

详情见: [https://github.com/pdf2htmlEX/pdf2htmlEX](https://github.com/pdf2htmlEX/pdf2htmlEX)

## 快速开始
```sh
# 拉取镜像
docker pull bytesfly/pdf2html-service:1.0.1

# 启动
docker run --name pdf2html -p 8686:8686 -d --rm bytesfly/pdf2html-service:1.0.1
```
使用:
```sh
curl -o html.zip --request POST 'localhost:8686/api/pdf2html' --form 'files=@/pdfs/example.pdf'
```
提醒一下: `/pdfs/example.pdf`指的是pdf文件所在的绝对路径  

在当前目录解压`html.zip`, 即可看到转换后的`html`文件以及`000-task.txt`。

## 构建镜像
```sh
# 下载代码
git clone https://github.com/bytesfly/pdf2html-service.git

# 进入项目
cd pdf2html-service

# 跳过单元测试打包
mvn clean package -DskipTests

# build docker image
docker build -t pdf2html-service:1.0.1 .
```

## 启动
```sh
docker run --name pdf2html -p 8686:8686 -d --rm pdf2html-service:1.0.1
```
如果需要格外设置一些参数的话, 可以启动docker的时候通过`-e`传进去: 
```sh
# 同时启动的最大子进程数, 需要根据系统的资源合理设置(默认15)
-e PDF2HTML_MAX_PROCESS=15

# 执行/usr/local/bin/pdf2htmlEX命令时最大超时时间,单位s表示秒(默认600s)
-e PDF2HTML_COMMAND_TIMEOUT=600s
```
即:
```sh
docker run --name pdf2html -p 8686:8686 -e PDF2HTML_MAX_PROCESS=10 -e PDF2HTML_COMMAND_TIMEOUT=60s -d --rm pdf2html-service:1.0.1
```
更多配置见: `resources`目录下的`application.yml`文件。

## Http接口

(1) 查看版本
```sh
curl http://localhost:8686/api/version
```

(2) 查看配置
```sh
curl http://localhost:8686/api/config
```

(3) 上传多个pdf, 并下载html压缩包

```sh
curl -o html.zip --request POST 'localhost:8686/api/pdf2html' --form 'files=@/pdfs/001.pdf' --form 'files=@/pdfs/002.pdf' --form 'files=@/pdfs/003.pdf'
```
提醒一下: `/pdfs/001.pdf`指的是pdf文件所在的绝对路径

(4) 查询程序暴露出来的metric

```sh
curl http://localhost:8686/api/metric
```

## 问题排查

```sh
# 进入容器
docker exec -it pdf2html bash

# 查看日志目录
cd /opt/pdf2html-service/logs

# 查看转换失败的pdf
cd /tmp/pdf2html-service/failed-pdfs

# 手动调用pdf2htmlEX命令转换pdf
pdf2htmlEX --help

```