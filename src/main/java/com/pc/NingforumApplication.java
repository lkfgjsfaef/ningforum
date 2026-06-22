package com.pc;
import java.lang.String;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan({"com.pc.dao", "com.app.dao"})
@ComponentScan({"com.pc", "com.app"})
public class NingforumApplication {
    public static void main(String[] args) { // 这里的String如果标红就是编译问题
        SpringApplication.run(NingforumApplication.class, args);
    }
}