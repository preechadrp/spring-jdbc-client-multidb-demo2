package com.mycom.springjdbcclientmultidbdemo2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.mycom.springjdbcclientmultidbdemo2.config.AppTimeZone;
import com.mycom.springjdbcclientmultidbdemo2.model.CustOrder;
import com.mycom.springjdbcclientmultidbdemo2.repository.CustOrderRepositoryDb2;

import lombok.extern.slf4j.Slf4j;

/**
 * ตัวอย่างคุม transaction ของ Database2 ด้วย PlatformTransactionManager / TransactionTemplate แทน @Transactional
 *
 * ต้องใช้ transactionManagerDb2 คู่กับ CustOrderRepositoryDb2 (jdbcClientDb2) เพราะสร้างจาก dataSourceDb2 ตัวเดียวกัน
 * transaction manager จะผูก Connection ของ dataSourceDb2 ไว้กับ thread แล้ว jdbcClientDb2 ก็ดึง Connection นั้นมาใช้
 * ถ้าไม่ใส่ @Qualifier จะได้ transactionManagerDb1 (@Primary) มาแทน และ rollback จะไม่มีผลกับ db2
 * (ดู test wrongTxManager_noRollback)
 *
 * ใช้ order_id 3001-3002 ลบทิ้งก่อนและหลังทุก test ไม่ชนกับข้อมูลของ Db2Test (2011-2020)
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Db2TestTx {

	private static final int ID1 = 3001;
	private static final int ID2 = 3002;

	@Autowired
	CustOrderRepositoryDb2 custOrderRepositoryDb2;

	@Autowired
	@Qualifier("transactionManagerDb2")
	PlatformTransactionManager txManagerDb2;

	@Autowired
	@Qualifier("transactionManagerDb1")
	PlatformTransactionManager txManagerDb1; // ใช้แค่ใน wrongTxManager_noRollback

	// TransactionTemplate เป็น thread-safe สร้างครั้งเดียวแล้วใช้ซ้ำได้
	// ในโค้ดจริงให้สร้างใน constructor ของ service (ดู CustOrderTxService ใน spring-jdbc-client-demo1)
	TransactionTemplate txTemplateDb2;
	TransactionTemplate readOnlyTxTemplateDb2;

	@BeforeEach
	void setUp() {
		txTemplateDb2 = new TransactionTemplate(txManagerDb2);
		txTemplateDb2.setTimeout(10); // วินาที

		// เทียบได้กับ @Transactional(transactionManager = "transactionManagerDb2", readOnly = true)
		readOnlyTxTemplateDb2 = new TransactionTemplate(txManagerDb2);
		readOnlyTxTemplateDb2.setReadOnly(true);

		cleanUp();
	}

	@AfterEach
	void cleanUp() {
		custOrderRepositoryDb2.deleteById(ID1);
		custOrderRepositoryDb2.deleteById(ID2);
	}

	private static CustOrder newOrder(int orderId, String amount) {
		return new CustOrder()
				.setOrderId(orderId)
				.setCustomerName("tx_customer" + orderId)
				.setTotalAmount(new BigDecimal(amount))
				.setOrderDate(LocalDate.now(AppTimeZone.ZONE_ID))
				.setInsertDatetime(Instant.now());
	}

	// =====================================================================
	// แบบที่ 1: ใช้ PlatformTransactionManager ตรงๆ
	// =====================================================================

	/** insert หลายรายการใน transaction เดียว: getTransaction() -> commit() / rollback() เขียนเอง */
	private int insertAllManual(List<CustOrder> custOrders) {
		// กำหนดคุณสมบัติของ transaction (เทียบกับ attribute ของ @Transactional)
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("Db2TestTx.insertAllManual");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		def.setTimeout(10);

		// เริ่ม transaction (ยืม Connection จาก pool mydb-2 แล้ว setAutoCommit(false))
		TransactionStatus status = txManagerDb2.getTransaction(def);

		int total = 0;
		try {
			for (CustOrder custOrder : custOrders) {
				total += custOrderRepositoryDb2.insert(custOrder);
			}
		} catch (RuntimeException | Error ex) {
			log.warn("rollback: {}", ex.getMessage());
			txManagerDb2.rollback(status);
			throw ex;
		}
		// commit อยู่นอก try: ถ้า commit เองพัง manager จัดการ rollback ให้แล้ว
		// ไม่ต้อง (และห้าม) เรียก rollback ซ้ำ ไม่งั้นจะได้ IllegalTransactionStateException
		txManagerDb2.commit(status);
		return total;
	}

	@Test
	@Order(1)
	void insertAllManual_commit() {
		log.info("====== start insertAllManual_commit");
		int rows = insertAllManual(List.of(newOrder(ID1, "1000"), newOrder(ID2, "2000")));

		assertEquals(2, rows);
		assertTrue(custOrderRepositoryDb2.findById(ID1).isPresent());
		assertTrue(custOrderRepositoryDb2.findById(ID2).isPresent());
	}

	@Test
	@Order(2)
	void insertAllManual_rollback() {
		log.info("====== start insertAllManual_rollback");
		// รายการที่ 2 order_id ซ้ำ -> DuplicateKeyException (subclass ของ DataIntegrityViolationException)
		assertThrows(DataIntegrityViolationException.class,
				() -> insertAllManual(List.of(newOrder(ID1, "1000"), newOrder(ID1, "2000"))));

		// รายการแรกที่ insert ไปแล้วต้องถูก rollback ด้วย
		assertTrue(custOrderRepositoryDb2.findById(ID1).isEmpty());
	}

	// =====================================================================
	// แบบที่ 2: ใช้ TransactionTemplate (แนะนำ)
	// lambda จบปกติ -> commit, โยน RuntimeException/Error -> rollback แล้วโยนต่อ
	// =====================================================================

	@Test
	@Order(3)
	void txTemplate_commit() {
		log.info("====== start txTemplate_commit");
		// execute() คืนค่าตามชนิดที่ lambda คืน (ที่นี่ Integer)
		Integer rows = txTemplateDb2.execute(status -> custOrderRepositoryDb2.insert(newOrder(ID1, "1000"))
				+ custOrderRepositoryDb2.insert(newOrder(ID2, "2000")));

		assertEquals(2, rows);
		assertTrue(custOrderRepositoryDb2.findById(ID2).isPresent());
	}

	@Test
	@Order(4)
	void txTemplate_rollback() {
		log.info("====== start txTemplate_rollback");
		// ไม่มีค่าคืน ใช้ executeWithoutResult
		assertThrows(DataIntegrityViolationException.class,
				() -> txTemplateDb2.executeWithoutResult(status -> {
					custOrderRepositoryDb2.insert(newOrder(ID1, "1000"));
					custOrderRepositoryDb2.insert(newOrder(ID1, "2000")); // order_id ซ้ำ
				}));

		assertTrue(custOrderRepositoryDb2.findById(ID1).isEmpty());
	}

	/** rollback โดยไม่ต้องโยน exception: แก้ยอดรวม แต่ถ้ายอดใหม่ต่ำกว่าครึ่งของยอดเดิม ให้ยกเลิก */
	private boolean updateTotalAmount(int orderId, BigDecimal newAmount) {
		Boolean committed = txTemplateDb2.execute(status -> {
			CustOrder custOrder = custOrderRepositoryDb2.findById(orderId).orElse(null);
			if (custOrder == null) {
				return false;
			}

			BigDecimal oldAmount = custOrder.getTotalAmount();
			custOrderRepositoryDb2.update(custOrder.setTotalAmount(newAmount));

			if (oldAmount != null && newAmount.compareTo(oldAmount.divide(BigDecimal.TWO)) < 0) {
				log.warn("rollback: new amount {} is less than half of {}", newAmount, oldAmount);
				status.setRollbackOnly(); // จบ lambda แล้วจะ rollback แทน commit
				return false;
			}
			return true;
		});
		return Boolean.TRUE.equals(committed); // execute() คืน @Nullable จึงไม่ unbox ตรงๆ
	}

	@Test
	@Order(5)
	void txTemplate_setRollbackOnly() {
		log.info("====== start txTemplate_setRollbackOnly");
		custOrderRepositoryDb2.insert(newOrder(ID1, "1000"));

		// 1000 -> 100 ต่ำกว่าครึ่ง -> setRollbackOnly() ยอดต้องยังเป็น 1000
		assertFalse(updateTotalAmount(ID1, new BigDecimal("100")));
		assertAmount(ID1, "1000");

		// 1000 -> 800 ผ่านเงื่อนไข -> commit
		assertTrue(updateTotalAmount(ID1, new BigDecimal("800")));
		assertAmount(ID1, "800");
	}

	@Test
	@Order(6)
	void txTemplate_readOnly() {
		log.info("====== start txTemplate_readOnly");
		custOrderRepositoryDb2.insert(newOrder(ID1, "1000"));

		List<CustOrder> datas = readOnlyTxTemplateDb2.execute(status -> custOrderRepositoryDb2.findAll());
		datas.forEach(data -> log.info("custOrder={}", data));
		assertTrue(datas.stream().anyMatch(o -> o.getOrderId() == ID1));
	}

	// =====================================================================
	// ข้อผิดพลาดที่พบบ่อย: ใช้ transaction manager ผิด database
	// =====================================================================

	@Test
	@Order(7)
	void wrongTxManager_noRollback() {
		log.info("====== start wrongTxManager_noRollback");
		// transaction เปิดที่ db1 แต่ repository เขียนลง db2
		// jdbcClientDb2 ไม่เจอ Connection ของ db2 ผูกไว้ จึงยืม Connection ใหม่แบบ auto-commit ทีละคำสั่ง
		TransactionTemplate wrongTemplate = new TransactionTemplate(txManagerDb1);

		assertThrows(DataIntegrityViolationException.class,
				() -> wrongTemplate.executeWithoutResult(status -> {
					custOrderRepositoryDb2.insert(newOrder(ID1, "1000"));
					custOrderRepositoryDb2.insert(newOrder(ID1, "2000")); // order_id ซ้ำ
				}));

		// รายการแรกไม่ถูก rollback เพราะ commit ไปแล้วตั้งแต่ตอน insert
		assertTrue(custOrderRepositoryDb2.findById(ID1).isPresent());
	}

	private void assertAmount(int orderId, String expected) {
		BigDecimal actual = custOrderRepositoryDb2.findById(orderId).orElseThrow().getTotalAmount();
		// DB คืน 1000.00 ใช้ compareTo เพื่อไม่สนจำนวนทศนิยม
		assertEquals(0, new BigDecimal(expected).compareTo(actual), "totalAmount=" + actual);
	}
}
