package com.jacky.agent;

import java.lang.instrument.Instrumentation;

public class AgentPre {

    /**
     * 该方法在main方法之前运行，与main方法运行在同一个jvm中
     * 并被同一个System ClassLoader装载
     * 被统一的案例策略(security policy)和上下文(context)管理
     * @param agentOps
     * @param inst
     */
    public static void premain(String agentOps, Instrumentation inst) {
        System.out.println("AgentPre premain方法开始执行...");

        Class[] classes = inst.getAllLoadedClasses();
        for (Class aClass : classes) {
            System.out.println(aClass.getName());
        }
        System.out.println("AgentPre premain方法执行结束...");
    }
}
