# cos-web-deploy
一款用于一键发布静态网站到腾讯云COS的maven插件。
## 开发背景
作为腾讯云COS（对象存储）的老用户，每月下行流量免费额度有10G。用来托管个人静态网站还是挺不错的。  
但是频繁发布站点还是很麻烦。每次都要手动选文件夹上传，而且不能定义文件夹里面哪些文件不上传。  
上传后想网站刷新缓存还得在内容分发网络里面输入网址刷新预热。  
总之不是很方便，所以有开发一键部署工具的想法。  
想过几种方案，还是觉得在IDE里面直接用maven插件的部署最为方便。在IDE里改完代码直接点发布，不用来回切换软件。
## 设计思路
- 通过腾讯云sdk，获取远程文件列表，通过比对md5，确定是否删除和覆盖远程文件。上传完成后调用刷新预热接口，清除网站缓存。  
- 参考gitignore设计忽略列表规则，由于!取非，优先级实现麻烦，而且不够直观，也不是很常用，故不实现。
## 使用说明
在pom.xml中添加以下内容，如果文件夹不是maven项目，可以手动添加pom.xml。  
插件会打印需要删除和上传的文件名日志到控制台。无需删除和上传的文件的日志级别为debug，如需查看，请添加-X 参数执行mvn指令。  
注意！注意！！注意！！！若本地上传目录包含当前`pom.xml`，请一定要配置忽略当前`pom.xml`，避免腾讯云密钥泄露。  
建议先执行pre-deploy预演发布指令，该指令不会实际删除和上传文件。可以用来查看有哪些文件会删除和上传，避免由于配置导致文件误删问题。
```xml
 <dependencies>
    <dependency>
        <groupId>com.iceolive</groupId>
        <artifactId>cos-web-deploy-maven-plugin</artifactId>
        <version>0.1.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
<build>
    <plugins>
        <plugin>
            <groupId>com.iceolive</groupId>
            <artifactId>cos-web-deploy-maven-plugin</artifactId>
            <version>0.1.0</version>
            <configuration>  
                <!--腾讯云secretId-->
                <secretId>xxxxxxxxxxxxx</secretId>
                <!--腾讯云secretKey-->
                <secretKey>xxxxxxxxxxxxxxx</secretKey>      
                <!--存储桶区域-->              
                <region>ap-guangzhou</region>
                <!--存储桶名称，格式:“name-appId”-->
                <bucketName>xxxx-xxxxx</bucketName>
                <!--本地上传目录,请填写绝对路径，或使用表达式-->
                <source>${project.basedir}</source>
                <!--cos的目标目录,空表示根目录,请按照cos目录格式填写，非空请以“/”结尾-->
                <target></target>
                <!--上传时是否先删除target目标目录下的所有文件（除远程忽略列表），格式:true/false-->
                <deleteRemoteFirst>false</deleteRemoteFirst>                   
                <!--本地上传忽略列表,多个使用“|”分割，规则基本同.gitignore(除了不支持!取非),实际本地忽略由localIgnore + remoteIgnore决定。支持通配符*-->
                <!--注意！注意！！注意！！！若本地上传目录包含当前pom.xml，请一定要配置忽略当前pom.xml，避免腾讯云密钥泄露。-->
                <localIgnore>*.iml|.idea/|/pom.xml</localIgnore>
                <!--远程忽略列表,不删除不添加不覆盖,多个使用“|”分割,规则基本同.gitignore(除了不支持!取非)-->
                <remoteIgnore>/upload</remoteIgnore>
                <!--你要刷新的网站目录，空则不刷新，腾讯云每日最多目录刷新次数100次-->
                <refreshPath>https://www.yoursite.com/</refreshPath>        
            </configuration>
            <executions>
                 <execution>
                    <id>deploy</id>
                    <phase>package</phase>
                    <goals>
                        <goal>deploy</goal>
                    </goals>
                </execution>
                <execution>
                    <id>pre-deploy</id>
                    <phase>compile</phase>
                    <goals>
                        <goal>pre-deploy</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
