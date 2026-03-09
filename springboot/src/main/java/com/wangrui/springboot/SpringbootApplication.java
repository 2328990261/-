// 必须改成新包名，和你实际的包结构一致
package com.wangrui.springboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 扫描Mapper包（确保路径正确）
@MapperScan("com.wangrui.springboot.mapper")
@SpringBootApplication
public class SpringbootApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootApplication.class, args);
    }
}