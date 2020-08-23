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
public class RemoteIgnoreFileFilter implements FileFilter {
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
        if (s.startsWith("\\\\")) {
            s = "^" + s;
        } else {
            s = "\\\\" + s;
        }
        return s;
    }

    public RemoteIgnoreFileFilter(String ignore) {
        this(ignore, null);
    }

    public RemoteIgnoreFileFilter(String ignore, String source) {
        if (!StringUtils.isBlank(source)) {
            this.source = new File(source).getPath();
        }
        if (!StringUtils.isBlank(ignore)) {
            for (String s : ignore.split("\\|")) {
                //由于远程无法区分是否文件夹，如果忽略规则没有"/"结尾，统一加一个用于匹配文件夹
                String a = escape(s);
                if (!a.endsWith("\\\\")) {
                    this.ignores.add(a+ "\\\\");
                    this.ignores.add(a + "$");
                }else{
                    this.ignores.add(a);
                    this.ignores.add(a.substring(0,a.length()-2)+"$");
                }
            }
        }
    }

    @Override
    public boolean accept(File pathname) {
        String path = pathname.getPath().substring(source.length());
        for (String s : this.ignores) {
            Pattern pattern = Pattern.compile(s);
            Matcher matcher;
            //判断文件
            matcher = pattern.matcher(path);
            if (matcher.find()) {
                log.debug(MessageFormat.format("忽略文件[{0}]", path));
                return false;
            }


        }
        return true;
    }
}
