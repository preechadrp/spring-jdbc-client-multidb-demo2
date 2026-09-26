- clone มาจาก spring-jdbc-api-multidb-demo1
- ทดสอบ spring boot JdbcClient (แทน JdbcTemplate)
- ทดสอบเชื่อม 2 database
- @Bean Methods ใช้ใน @Configuration Classes
- ใช้ java.time.Instant สำหรับ insert_datetime + timezone Asia/Bangkok
  * JVM default timezone = Asia/Bangkok (ตั้งใน main)
  * DB session: SET time_zone = '+07:00' (Hikari connection-init-sql)
  * ไม่ใช้ LocalDateTime: ส่ง/อ่าน Instant กับ driver โดยตรง
    jdbc-url กำหนด connectionTimeZone=Asia/Bangkok -> DATETIME ใน DB เป็นเวลา Bangkok
  * JSON แสดงผลเป็นเวลา Bangkok (spring.jackson.time-zone + @JsonFormat)
- JdbcClient ไม่มี batchUpdate จึงทำ insert หลายรายการใน @Transactional แทน
