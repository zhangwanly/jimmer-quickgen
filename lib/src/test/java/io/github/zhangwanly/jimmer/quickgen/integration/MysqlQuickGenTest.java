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
        dataSource.setURL("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8");
        dataSource.setUser(user);
        dataSource.setPassword(password);

        QuickGenConfig config = QuickGenConfig.builder()
                .outputDir(Paths.get("src/main/generated"))
                .basePackage("io.github.zhangwanly.jimmer.entity")
                .generationType(GenerationType.USER)
                .baseEntityConfig(entity -> {
                    entity.columnPatterns("id", "create_time", "update_time", "is_deleted");
                })
                .build();

        QuickGen.generate(dataSource, config);
    }

}
