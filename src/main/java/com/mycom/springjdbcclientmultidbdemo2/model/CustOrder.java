package com.mycom.springjdbcclientmultidbdemo2.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mycom.springjdbcclientmultidbdemo2.config.AppTimeZone;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CustOrder {

	private Integer orderId;
	private String customerName;
	private BigDecimal totalAmount;

	@JsonFormat(pattern = "yyyy-MM-dd") // กำหนดรูปแบบวันที่ในการแสดงผล
	private LocalDate orderDate;

	// Instant = จุดเวลา (UTC) แสดงผล JSON เป็นเวลา Asia/Bangkok
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSXXX", timezone = AppTimeZone.ZONE_ID_STR)
	private Instant insertDatetime;

}
