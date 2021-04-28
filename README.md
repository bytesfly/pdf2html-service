
Convert PDF to HTML without losing text or format.

用`springboot`把`pdf2htmlEX`命令行工具包装为`web`服务, 使得PDF转HTML更为方便。 

详情见: [https://github.com/pdf2htmlEX/pdf2htmlEX](https://github.com/pdf2htmlEX/pdf2htmlEX)

## 构建镜像
```sh
# 下载代码
git clone https://github.com/iflyendless/pdf2html-service.git

# 进入项目
cd pdf2html-service

# 跳过单元测试打包
mvn clean package -DskipTests

# build docker image
docker build -t pdf2html-service:1.0.0 .
```

## 启动
```sh
docker run --name pdf2html -p 8686:8686 -d --rm pdf2html-service:1.0.0
```
如果需要格外设置一些参数的话, 可以启动docker的时候通过`-e`传进去: 
```sh
# 同时启动的最大子进程数, 需要根据系统的资源合理设置(默认15)
-e PDF2HTML_MAX_PROCESS=15

# 执行/usr/local/bin/pdf2htmlEX命令时最大超时时间,单位s表示秒(默认600s)
-e PDF2HTML_COMMAND_TIMEOUT=600s
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
curl -o html.zip --location --request POST 'localhost:8686/api/pdf2html' --form 'files=@/pdfs/001.pdf' --form 'files=@/pdfs/002.pdf' --form 'files=@/pdfs/003.pdf'
```