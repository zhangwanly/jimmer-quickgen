package io.github.zhangwanly.jimmer.quickgen.integration;

import com.mysql.cj.jdbc.MysqlDataSource;
import io.github.zhangwanly.jimmer.quickgen.QuickGen;
import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import org.babyfish.jimmer.sql.GenerationType;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class MysqlQuickGenTest {

    @Test
    public void gen() {
        String host = System.getenv().getOrDefault("MYSQL_HOST", "localhost");
        String port = System.getenv().getOrDefault("MYSQL_PORT", "3306");
        String db = System.getenv().getOrDefault("MYSQL_DB", "pegasus");
        String user = System.getenv().getOrDefault("MYSQL_USER", "root");
        String password = System.getenv().getOrDefault("MYSQL_PASSWORD", "");

        if (password.isEmpty()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "MYSQL_PASSWORD not set, skipping");
        }

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8");
        dataSource.setUser(user);
        dataSource.setPassword(password);

        QuickGenConfig config = QuickGenConfig.builder()
                .outputDir(Paths.get("src/main/generated"))
                .basePackage("io.github.zhangwanly.jimmer.entity")
                .generationType(GenerationType.USER)
                .baseEntityConfig(entity -> {
                    entity.columnPatterns("id", "create_time", "update_time", "is_deleted");
                })
                .tableRefOverride("order_info", "user_id", "user_info")
                .tableRefOverride("order_info", "coupon_id", "coupon_info")
                .tableRefOverride("sys_role_menu", "menu_id", "sys_menu")
                .tableRefOverride("order_item", "order_id", "order_info")
                .tableRefOverride("order_log", "order_id", "order_info")
                .tableRefOverride("sys_role_menu", "role_id", "sys_role")
                .tableRefOverride("sys_user_role", "role_id", "sys_role")
                .tableRefOverride("order_item", "sku_id", "product_sku")
                .tableRefOverride("user_browse_history", "sku_id", "product_sku")
                .tableRefOverride("user_browse_history", "user_id", "user_info")
                .tableRefOverride("user_collect", "sku_id", "product_sku")
                .tableRefOverride("user_collect", "user_id", "user_info")
                .tableRefOverride("payment_info", "user_id", "user_info")
                .tableRefOverride("sys_user_role", "user_id", "sys_user")
                .tableRefOverride("user_address", "user_id", "user_info")
                .tableRefOverride("coupon_user", "coupon_id", "coupon_info")
                .tableRefOverride("coupon_user", "order_id", "order_info")
                .tableRefOverride("coupon_user", "user_id", "user_info")
                .tableRefOverride("coupon_range", "coupon_id", "coupon_info")
                .build();

        QuickGen.generate(dataSource, config);
    }

}
