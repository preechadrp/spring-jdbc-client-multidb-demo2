package com.mycom.springjdbcclientmultidbdemo2;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.mycom.springjdbcclientmultidbdemo2.config.AppTimeZone;

@SpringBootApplication
public class SpringJdbcClientMultidbDemo2Application {

	public static void main(String[] args) {
		// กำหนด default timezone ของ JVM เป็น Asia/Bangkok ก่อน Spring เริ่มทำงาน
		TimeZone.setDefault(TimeZone.getTimeZone(AppTimeZone.ZONE_ID));
		SpringApplication.run(SpringJdbcClientMultidbDemo2Application.class, args);
	}

}
