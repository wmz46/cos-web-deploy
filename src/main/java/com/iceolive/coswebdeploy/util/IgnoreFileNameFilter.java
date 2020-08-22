package com.iceolive.coswebdeploy.util;


import com.iceolive.coswebdeploy.config.CosConfig;
import com.iceolive.coswebdeploy.service.CosService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wangmianzhe
 */
public class IgnoreFileNameFilter implements FilenameFilter {
    private List<String> ignores = new ArrayList<>();
    private String source = "";


    private String escape(String s) {
        //去空格
        s = s.trim();
        //规范斜杠
        s = "/" + s;
        s = s.replace("/", "\\\\");
        s = s.replaceAll("\\\\+", "\\\\\\\\");

        s = s.replaceAll("\\.", "\\\\.");
        s = s.replaceAll("\\*\\*", "\\<");
        s = s.replaceAll("\\*", "[^\\\\\\\\]*");
        s = s.replaceAll("\\<", ".*");
        s = s.replaceAll("\\?", "[^\\\\\\\\]?");
        return s;
    }

    public IgnoreFileNameFilter(String ignore) {
        this(ignore, null);
    }

    public IgnoreFileNameFilter(String ignore, String source) {
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
    public boolean accept(File dir, String name) {
        for (String s : this.ignores) {
            Pattern pattern = Pattern.compile(s);
            Matcher matcher;
            String path = "";
            if(dir!=null) {
                path = dir.getPath().substring(source.length());
            }
            matcher = pattern.matcher(path);
            if(matcher.find()){
                return  false;
            }
            path +="\\";
            matcher = pattern.matcher(path);
            if(matcher.find()){
                return  false;
            }
            path+=name;
            matcher = pattern.matcher(path);
            if(matcher.find()){
                return  false;
            }
        }
        return true;
    }
}
