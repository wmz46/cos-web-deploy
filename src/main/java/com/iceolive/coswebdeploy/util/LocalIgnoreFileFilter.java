package com.iceolive.coswebdeploy.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wangmianzhe
 */
@Slf4j
public class LocalIgnoreFileFilter implements FileFilter {
    private List<String> ignores = new ArrayList<>();
    private String source = "";


    private String escape(String s) {
        //去空格
        s = s.trim();
        //规范斜杠
        s = s.replace("/", "\\\\");
        s = s.replaceAll("\\\\+", "\\\\\\\\");

        s = s.replaceAll("\\.", "\\\\.");
        s = s.replaceAll("\\*\\*", "\\<");
        s = s.replaceAll("\\*", "[^\\\\\\\\]*");
        s = s.replaceAll("\\<", ".*");
        s = s.replaceAll("\\?", "[^\\\\\\\\]?");
        s+="$";
        if (s.startsWith("\\\\")) {
            s = "^" + s;
        } else {
            s = "\\\\" + s;
        }
        return s;
    }

    public LocalIgnoreFileFilter(String ignore) {
        this(ignore, null);
    }

    public LocalIgnoreFileFilter(String ignore, String source) {
        if (!StringUtils.isBlank(source)) {
            this.source = new File(source).getPath();
        }
        if (!StringUtils.isBlank(ignore)) {
            for (String s : ignore.split("\\|")) {
                this.ignores.add(escape(s));
            }
        }
    }

    @Override
    public boolean accept(File pathname) {
        for (String s : this.ignores) {
            Pattern pattern = Pattern.compile(s);
            Matcher matcher;
            String path =   pathname.getPath().substring(source.length());
            if(pathname.isDirectory()){
                //判断文件夹
                path += "\\";
                matcher = pattern.matcher(path);
                if (matcher.find()) {
                    log.debug(MessageFormat.format("忽略文件夹[{0}]",path));
                    return false;
                }
            }else{
                //判断文件
                matcher = pattern.matcher(path);
                if (matcher.find()) {
                    log.debug(MessageFormat.format("忽略文件[{0}]",path));
                    return false;
                }
            }



        }
        return true;
    }
}
