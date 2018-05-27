package com.alipay.sofa.rpc;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MainTest {
    @Test
    public void test1() {

        Map<String, String> map = new HashMap<String, String>();

        map.put("12", "22");

        System.out.println(map.putIfAbsent("12", "23"));
        System.out.println(map.get("12"));

        System.out.println(map.putIfAbsent("1", "2"));
        System.out.println(map.get("1"));

    }
}
