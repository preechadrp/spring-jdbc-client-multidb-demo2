package com.mycom.springjdbcclientmultidbdemo2;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mycom.springjdbcclientmultidbdemo2.config.AppTimeZone;
import com.mycom.springjdbcclientmultidbdemo2.model.CustOrder;
import com.mycom.springjdbcclientmultidbdemo2.repository.CustOrderRepositoryDb1;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Db1Test {
	@Autowired
	public CustOrderRepositoryDb1 custOrderRepositoryDb1;

	private final int start = 2011;
	private final int to = start + 5;

	@Test
	@Order(1)
	void delete() {
		log.info("====== start delete");
		for (int idx = start; idx < to + 5; idx++) {
			int eff_row = custOrderRepositoryDb1.deleteById(idx);
			log.info("delete idx={}, effect row={}", idx, eff_row);
		}
	}

	@Test
	@Order(2)
	void insert() {
		log.info("====== start insert");
		for (int idx = start; idx < to; idx++) {
			var custorder = new CustOrder()
					.setOrderId(idx)
					.setCustomerName("customer_name" + idx)
					.setTotalAmount(new BigDecimal(idx + "00"))
					.setOrderDate(LocalDate.now(AppTimeZone.ZONE_ID))
					.setInsertDatetime(Instant.now());

			custOrderRepositoryDb1.insert(custorder);
		}
	}

	@Test
	@Order(3)
	void insertByBatch() {
		log.info("====== start insertByBatch");
		int rows = custOrderRepositoryDb1.insertOrdersByBatch(to, 5);
		log.info("insertByBatch rows={}", rows);
	}

	@Test
	@Order(4)
	void findAll() {
		log.info("====== start findAll");
		var datas = custOrderRepositoryDb1.findAll();
		for (CustOrder custOrder : datas) {
			log.info("custOrder={}, insertDatetime(Bangkok)={}", custOrder,
					custOrder.getInsertDatetime() == null ? null
							: custOrder.getInsertDatetime().atZone(AppTimeZone.ZONE_ID));
		}
	}

	@Test
	@Order(5)
	void findById() {
		log.info("====== start findById");
		custOrderRepositoryDb1.findById(this.start).ifPresentOrElse(
				data -> log.info("custOrder={}", data),
				() -> log.info("Not found data."));
	}

	@Test
	@Order(6)
	void findByCustomerName() {
		log.info("====== start findByCustomerName");
		var datas = custOrderRepositoryDb1.findByCustomerName("customer_name" + start);
		if (datas.isEmpty()) {
			log.info("Not found data.");
			return;
		}
		for (CustOrder data : datas) {
			log.info("custOrder={}", data);
		}
	}

	@Test
	@Order(7)
	void update() {
		log.info("====== start update");
		custOrderRepositoryDb1.findById(this.start).ifPresentOrElse(data -> {
			data.setTotalAmount(new BigDecimal("200"))
				.setInsertDatetime(Instant.now());
			int rows = custOrderRepositoryDb1.update(data);
			log.info("update rows={}, data={}", rows, data);
		}, () -> log.info("Not found data."));
	}
}
