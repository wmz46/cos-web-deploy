package com.iceolive.coswebdeploy.service;

import com.iceolive.coswebdeploy.config.CosConfig;
import com.iceolive.coswebdeploy.util.IgnoreFileNameFilter;
import com.iceolive.coswebdeploy.util.Md5Util;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import com.tencentcloudapi.cdn.v20180606.CdnClient;
import com.tencentcloudapi.cdn.v20180606.models.PurgePathCacheRequest;
import com.tencentcloudapi.cdn.v20180606.models.PurgePathCacheResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wangmianzhe
 */
@Slf4j
public class CosService {
    private CosConfig cosConfig;

    private COSClient cosClient;

    public CosService(CosConfig cosConfig) {
        this.cosConfig = cosConfig;
        initCosClient();
    }

    /**
     * 获取cos客户端
     *
     * @return
     */
    private void initCosClient() {
        if (cosClient == null) {
            log.info("初始化cos客户端...");
            // 1 初始化用户身份信息（secretId, secretKey）。
            String secretId = cosConfig.getSecretId();
            String secretKey = cosConfig.getSecretKey();
            COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
            // 2 设置 bucket 的区域, COS 地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
            // clientConfig 中包含了设置 region, https(默认 http), 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
            Region region = new Region(cosConfig.getRegion());
            ClientConfig clientConfig = new ClientConfig(region);
            // 3 生成 cos 客户端。
            cosClient = new COSClient(cred, clientConfig);
            log.info("初始化cos客户端成功。");
        }
    }

    /**
     * 查询存储桶列表
     *
     * @return
     */
    private List<Bucket> getBucketList() {
        List<Bucket> buckets = cosClient.listBuckets();
        for (Bucket bucketElement : buckets) {
            String bucketName = bucketElement.getName();
            String bucketLocation = bucketElement.getLocation();
        }
        return buckets;
    }

    /**
     * 上传文件
     *
     * @param key
     * @param localFilePath
     * @return
     */
    private PutObjectResult putObject(String key, String localFilePath) {
        // 指定要上传的文件
        File localFile = new File(localFilePath);
        // 指定要上传到的存储桶
        String bucketName = cosConfig.getBucketName();
        // 指定要上传到 COS 上对象键
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        log.info(MessageFormat.format("上传文件[{0}]到远程[{1}]", localFilePath, key));
        return putObjectResult;
    }

    /**
     * 下载文件
     *
     * @param key
     * @param localFilePath
     * @return
     */
    private ObjectMetadata getObject(String key, String localFilePath) {
        // Bucket的命名格式为 BucketName-APPID ，此处填写的存储桶名称必须为此格式
        String bucketName = cosConfig.getBucketName();
        // 方法2 下载文件到本地
        String outputFilePath = localFilePath;
        File downFile = new File(outputFilePath);
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        return cosClient.getObject(getObjectRequest, downFile);

    }

    /**
     * 获取对象列表
     *
     * @param prefix
     * @return
     */
    private List<COSObjectSummary> getObjectList(String prefix) {
        List<COSObjectSummary> cosObjectSummaries = new ArrayList<>();
        // Bucket的命名格式为 BucketName-APPID ，此处填写的存储桶名称必须为此格式
        String bucketName = cosConfig.getBucketName();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        // 设置bucket名称
        listObjectsRequest.setBucketName(bucketName);
        // prefix表示列出的object的key以prefix开始
        listObjectsRequest.setPrefix(prefix);
        // deliter表示分隔符, 设置为/表示列出当前目录下的object, 设置为空表示列出所有的object
        listObjectsRequest.setDelimiter("");
        // 设置最大遍历出多少个对象, 一次listobject最大支持1000
        listObjectsRequest.setMaxKeys(1000);
        ObjectListing objectListing = null;
        do {
            try {
                objectListing = cosClient.listObjects(listObjectsRequest);
            } catch (CosServiceException e) {
                throw new RuntimeException(e);
            } catch (CosClientException e) {
                throw new RuntimeException(e);
            }
            // common prefix表示表示被delimiter截断的路径, 如delimter设置为/, common prefix则表示所有子目录的路径
            List<String> commonPrefixs = objectListing.getCommonPrefixes();

            // object summary表示所有列出的object列表
            cosObjectSummaries.addAll(objectListing.getObjectSummaries());
            String nextMarker = objectListing.getNextMarker();
            listObjectsRequest.setMarker(nextMarker);
        } while (objectListing.isTruncated());
        return cosObjectSummaries;
    }

    /**
     * 删除对象
     *
     * @param key
     */
    private void deleteObject(String key) {
        // Bucket的命名格式为 BucketName-APPID ，此处填写的存储桶名称必须为此格式
        String bucketName = cosConfig.getBucketName();
        cosClient.deleteObject(bucketName, key);
        log.info(MessageFormat.format("删除远程文件[{0}]", key));
    }

    public List<String> getAllFiles() {
        FilenameFilter filenameFilter = new IgnoreFileNameFilter(cosConfig.getLocalIgnore() + "|" + cosConfig.getRemoteIgnore(), cosConfig.getSource());
        return getAllFiles(new File(cosConfig.getSource()), filenameFilter);
    }

    private List<String> getAllFiles(File root, FilenameFilter filenameFilter) {
        List<String> result = new ArrayList<>();

        File[] files = root.listFiles(filenameFilter);
        for (File file : files) {
            if (file.isDirectory()) {
                result.addAll(getAllFiles(file, filenameFilter));
            } else {
                result.add(file.getPath());
            }
        }
        return result;
    }

    /**
     * 刷新目录
     */
    public void refreshCdn() {
        if (!StringUtils.isBlank(cosConfig.getRefreshPath())) {
            try {
                String refrushUrl = cosConfig.getRefreshPath();
                if (!refrushUrl.endsWith("/")) {
                    refrushUrl += "/";
                }

                Credential cred = new Credential(cosConfig.getSecretId(), cosConfig.getSecretKey());

                HttpProfile httpProfile = new HttpProfile();
                httpProfile.setEndpoint("cdn.tencentcloudapi.com");

                ClientProfile clientProfile = new ClientProfile();
                clientProfile.setHttpProfile(httpProfile);

                CdnClient client = new CdnClient(cred, cosConfig.getRegion(), clientProfile);

                String params = "{\"Paths\":[\"" + refrushUrl + "\"],\"FlushType\":\"flush\"}";
                PurgePathCacheRequest req = PurgePathCacheRequest.fromJsonString(params, PurgePathCacheRequest.class);

                PurgePathCacheResponse resp = client.PurgePathCache(req);
                log.info(MessageFormat.format("刷新目录[{0}]", refrushUrl));
            } catch (TencentCloudSDKException e) {
                log.error(e.toString(), e);
            }

        }
    }

    /**
     * 发布
     */
    public void deploy() {

        int deleteCount = 0;
        int uploadCount = 0;
        //遍历本地source下目录
        List<String> localFiles = getAllFiles();
        //远程文件列表
        List<COSObjectSummary> remoteList = getObjectList(cosConfig.getTarget().replace("\\", "/"));

        //本地上传目录
        File source = new File(cosConfig.getSource());

        //远程忽略列表
        FilenameFilter filenameFilter = new IgnoreFileNameFilter(cosConfig.getRemoteIgnore());
        //上传前是否删除cos文件
        if (cosConfig.getDeleteRemoteFirst().equals(true)) {
            log.info("开始删除远程文件...");
            for (COSObjectSummary item : remoteList) {
                File file = new File(item.getKey());
                //不在忽略列表里面的
                if (filenameFilter.accept(file.getParentFile(), file.getName())) {
                    //检查本地是否有文件，且md5一致，一致则不删除
                    String f = localFiles.stream().filter(m -> item.getKey().equals((cosConfig.getTarget() + m.replace(source.getPath() + "\\", "")).replace("\\", "/"))).findFirst().orElse(null);
                    if (!StringUtils.isBlank(f) && Md5Util.getMD5(new File(f)).equals(item.getETag())) {
                        log.debug(MessageFormat.format("远程文件[{0}]无需删除", item.getKey()));
                        continue;
                    }
                    deleteObject(item.getKey());
                    deleteCount++;
                }
            }
        }
        log.info("开始上传本地文件...");
        for (String file : localFiles) {
            String key = (cosConfig.getTarget() + file.replace(source.getPath() + "\\", "")).replace("\\", "/");
            COSObjectSummary cosObjectSummary = remoteList.stream().filter(m -> m.getKey().equals(key)).findFirst().orElse(null);
            if (cosObjectSummary != null && cosObjectSummary.getETag().equals(Md5Util.getMD5(new File(file)))) {
                //如果远程文件存在且md5一致，则无需上传
                log.debug(MessageFormat.format("文件[{0}]和远程[{1}]一致，无需上传", file, key));
                continue;
            }
            putObject(key, file);
            uploadCount++;
        }
        log.info(MessageFormat.format("本次合计删除{0}个文件，上传{1}个文件",deleteCount,uploadCount));
        //刷新目录
        refreshCdn();
    }
}
