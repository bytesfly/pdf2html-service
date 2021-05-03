
# pdf2htmlex image
FROM pdf2htmlex/pdf2htmlex:0.18.8.rc1-master-20200630-Ubuntu-bionic-x86_64

ENV TZ='CST-8'
ENV LANG C.UTF-8

# apt
RUN sed -i s@/archive.ubuntu.com/@/mirrors.aliyun.com/@g /etc/apt/sources.list
RUN apt-get clean && apt-get update
RUN apt-get install -y vim curl htop net-tools

# vim
RUN echo "set fileencodings=utf-8,ucs-bom,gb18030,gbk,gb2312,cp936" >> /etc/vim/vimrc
RUN echo "set termencoding=utf-8" >> /etc/vim/vimrc
RUN echo "set encoding=utf-8" >> /etc/vim/vimrc

# jdk
ADD https://enos.itcollege.ee/~jpoial/allalaadimised/jdk8/jdk-8u291-linux-x64.tar.gz /tmp/
RUN tar -zxf /tmp/jdk-*.tar.gz -C /opt/ && rm -f /tmp/jdk-*.tar.gz && mv /opt/jdk* /opt/jdk

ENV JAVA_HOME /opt/jdk
ENV PATH ${JAVA_HOME}/bin:$PATH

# pdf2html-service
COPY target/pdf2html-service-*.tar.gz /tmp/
RUN tar -zxf /tmp/pdf2html-service-*.tar.gz -C /opt/ && rm -f /tmp/pdf2html-service-*.tar.gz

ENTRYPOINT [""]
WORKDIR /opt/pdf2html-service
CMD ["bash","-c","./start.sh && tail -f /dev/null"]
