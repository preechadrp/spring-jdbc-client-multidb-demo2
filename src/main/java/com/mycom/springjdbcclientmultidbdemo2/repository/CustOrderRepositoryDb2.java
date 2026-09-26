package com.mycom.springjdbcclientmultidbdemo2.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.springjdbcclientmultidbdemo2.config.AppTimeZone;
import com.mycom.springjdbcclientmultidbdemo2.model.CustOrder;

@Repository
public class CustOrderRepositoryDb2 {

	private final JdbcClient jdbcClient;

	//ต้องระบุ @Qualifier("jdbcClientDb2") ด้วยสำหรับตัวที่ไม่ใช่ @Primary
	public CustOrderRepositoryDb2(@Qualifier("jdbcClientDb2") JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	// DATETIME (เวลา Asia/Bangkok) -> Instant (driver แปลงตาม connectionTimeZone)
	private final RowMapper<CustOrder> rowMapper = (rs, rowNum) -> new CustOrder()
			.setOrderId(rs.getInt("order_id"))
			.setCustomerName(rs.getString("customer_name"))
			.setTotalAmount(rs.getBigDecimal("total_amount"))
			.setOrderDate(rs.getObject("order_date", LocalDate.class))
			.setInsertDatetime(rs.getObject("insert_datetime", Instant.class));

	public int insert(CustOrder custorder) {

		String sql = """
				INSERT INTO cust_order
				(order_id, customer_name, total_amount, order_date, insert_datetime)
				VALUES (:orderId, :customerName, :totalAmount, :orderDate, :insertDatetime)
				""";

		return jdbcClient.sql(sql)
				.param("orderId", custorder.getOrderId())
				.param("customerName", custorder.getCustomerName())
				.param("totalAmount", custorder.getTotalAmount())
				.param("orderDate", custorder.getOrderDate())
				.param("insertDatetime", custorder.getInsertDatetime()) // Instant -> DATETIME (Asia/Bangkok)
				.update();
	}

	public List<CustOrder> findAll() {
		return jdbcClient.sql("SELECT * FROM cust_order order by order_id")
				.query(rowMapper)
				.list();
	}

	public Optional<CustOrder> findById(int orderId) {
		return jdbcClient.sql("SELECT * FROM cust_order where order_id = :orderId")
				.param("orderId", orderId)
				.query(rowMapper)
				.optional();
	}

	public List<CustOrder> findByCustomerName(String customerName) {
		return jdbcClient.sql("SELECT * FROM cust_order where customer_name = :customerName order by order_id")
				.param("customerName", customerName)
				.query(rowMapper)
				.list();
	}

	public int update(CustOrder custorder) {

		String sql = """
				update cust_order set
				customer_name = :customerName, total_amount = :totalAmount,
				order_date = :orderDate, insert_datetime = :insertDatetime
				where order_id = :orderId
				""";

		return jdbcClient.sql(sql)
				.param("customerName", custorder.getCustomerName())
				.param("totalAmount", custorder.getTotalAmount())
				.param("orderDate", custorder.getOrderDate())
				.param("insertDatetime", custorder.getInsertDatetime())
				.param("orderId", custorder.getOrderId())
				.update();
	}

	public int deleteById(int orderId) {
		return jdbcClient.sql("delete from cust_order where order_id = :orderId")
				.param("orderId", orderId)
				.update();
	}

	/**
	 * ตัวอย่าง insert หลายรายการ
	 * JdbcClient ไม่มี batchUpdate โดยตรง จึงวน insert ภายใน transaction เดียวกัน
	 * (ทั้งหมดสำเร็จ หรือ rollback ทั้งหมด)
	 */
	@Transactional(transactionManager = "transactionManagerDb2")
	public int insertOrdersByBatch(int start, int size) {
		Instant now = Instant.now();
		LocalDate today = LocalDate.now(AppTimeZone.ZONE_ID);
		int total = 0;
		for (int i = 0; i < size; i++) {
			int idx = start + i;
			total += insert(new CustOrder()
					.setOrderId(idx)
					.setCustomerName("customer_name" + idx)
					.setTotalAmount(new BigDecimal(idx + "00"))
					.setOrderDate(today)
					.setInsertDatetime(now));
		}
		return total;
	}

}
