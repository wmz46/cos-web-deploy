package com.iceolive.coswebdeploy.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangmianzhe
 */
@Data
public class CosConfig {
    private String secretId;
    private String secretKey;
    private String region;
    private String bucketName;
    private String source;
    private String target;
    public String getTarget(){
        if(target == null){
            return "";
        }else{
            return target;
        }
    }
    private String localIgnore;

    private Boolean deleteRemoteFirst;
    private String remoteIgnore;

    public String getLocalIgnore() {
        if(localIgnore == null){
            return "";
        }else{
            return  localIgnore;
        }
    }

    public String getRemoteIgnore() {
        if( remoteIgnore == null){
            return "";
        }else{
            return remoteIgnore;
        }
    }
    private String refreshPath;
}
