package com.iceolive.coswebdeploy.mojo;

import com.iceolive.coswebdeploy.config.CosConfig;
import com.iceolive.coswebdeploy.service.CosService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.sisu.Parameters;

import java.text.MessageFormat;

/**
 * @author wangmianzhe
 */
@Data
@Slf4j
@Mojo(name = "deploy", threadSafe = true, defaultPhase = LifecyclePhase.COMPILE)
public class DeployMojo extends AbstractMojo {

    /**
     * secretId
     */
    @Parameter(property = "secretId",required = true)
    private String secretId;
    /**
     * secretKey
     */
    @Parameter(property = "secretKey",required = true)
    private String secretKey;
    /**
     * 区域
     */
    @Parameter(property = "region",required = true)
    private String region;
    /**
     * 存储桶名称
     */
    @Parameter(property = "bucketName",required = true)
    private String bucketName;
    /**
     * 本地目录
     */
    @Parameter(property = "source",required = true)
    private String source;
    /**
     * cos目标目录,空字符串表示根目录
     */
    @Parameter(property = "target")
    private String target;
    /**
     * 本地上传忽略列表,多个使用“|”分割，实际本地忽略由localIgnore + remoteIgnore决定
     */
    @Parameter(property = "localIgnore")
    private String localIgnore;
    /**
     * 上传时是否先删除target目标目录下的所有文件（除远程忽略列表）
     */
    @Parameter(property = "deleteRemoteFirst")
    private Boolean deleteRemoteFirst;
    /**
     * 远程忽略列表,不删除不添加不覆盖,多个使用“|”分割
     */
    @Parameter(property = "remoteIgnore")
    private String remoteIgnore;
    /**
     * 刷新url目录
     */
    @Parameter(property = "refreshPath")
    private String refreshPath;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            long start = System.currentTimeMillis();
            log.info("开始发布...");
            CosConfig cosConfig = new CosConfig();
            cosConfig.setSecretId(secretId);
            cosConfig.setSecretKey(secretKey);
            cosConfig.setRegion(region);
            cosConfig.setBucketName(bucketName);
            cosConfig.setSource(source);
            cosConfig.setTarget(target);
            cosConfig.setLocalIgnore(localIgnore);
            cosConfig.setDeleteRemoteFirst(deleteRemoteFirst);
            cosConfig.setRemoteIgnore(remoteIgnore);
            cosConfig.setRefreshPath(refreshPath);
            CosService cosService = new CosService(cosConfig);
            cosService.deploy();
            getLog().info(MessageFormat.format("发布完成,耗时{0}s。", (System.currentTimeMillis()-start)/1000));

        } catch (Exception e) {
            log.error("发布失败！",e);
        }
    }
}
