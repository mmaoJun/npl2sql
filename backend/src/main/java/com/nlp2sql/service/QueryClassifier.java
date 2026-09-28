package com.nlp2sql.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class QueryClassifier {

    public enum QueryType {
        SIMPLE_SELECT,
        WHERE_FILTER,
        AGGREGATION,
        JOIN,
        SUBQUERY,
        TOP_N,
        TREND,
        FUZZY
    }

    private static final Map<QueryType, List<String>> KEYWORD_PATTERNS = new EnumMap<>(QueryType.class);

    static {
        KEYWORD_PATTERNS.put(QueryType.AGGREGATION,
                List.of("统计", "数量", "总数", "平均", "最大", "最小", "汇总", "占比", "多少", "合计", "总和"));
        KEYWORD_PATTERNS.put(QueryType.TOP_N,
                List.of("前几", "前10", "前5", "前3", "top", "排名", "最多", "最高", "最低", "最大", "最小"));
        KEYWORD_PATTERNS.put(QueryType.TREND,
                List.of("趋势", "每月", "每天", "每年", "变化", "增长", "按月", "按天", "按年", "时间线"));
        KEYWORD_PATTERNS.put(QueryType.FUZZY,
                List.of("包含", "类似", "大约", "大概", "像", "模糊", "含有", "带有"));
        KEYWORD_PATTERNS.put(QueryType.JOIN,
                List.of("的订单", "的用户", "关联", "对应", "所属", "购买", "下单"));
        KEYWORD_PATTERNS.put(QueryType.SUBQUERY,
                List.of("超过平均", "高于平均", "低于平均", "比平均", "最贵", "最便宜"));
        KEYWORD_PATTERNS.put(QueryType.WHERE_FILTER,
                List.of("大于", "小于", "等于", "不等于", "超过", "不足", "在", "之间", "或者", "并且"));
    }

    public QueryType classify(String nlInput) {
        if (nlInput == null || nlInput.isBlank()) {
            return QueryType.SIMPLE_SELECT;
        }

        String input = nlInput.toLowerCase();

        // 按优先级检查
        for (QueryType type : List.of(
                QueryType.SUBQUERY, QueryType.TOP_N, QueryType.TREND,
                QueryType.AGGREGATION, QueryType.FUZZY, QueryType.JOIN,
                QueryType.WHERE_FILTER)) {
            for (String keyword : KEYWORD_PATTERNS.get(type)) {
                if (input.contains(keyword.toLowerCase())) {
                    return type;
                }
            }
        }

        return QueryType.SIMPLE_SELECT;
    }

    public List<String> getFewShotExamples(QueryType type) {
        return switch (type) {
            case AGGREGATION -> List.of(
                    "问题: 统计每个部门的用户数量\nSQL: SELECT dept_id, COUNT(*) AS user_count FROM users GROUP BY dept_id ORDER BY user_count DESC;",
                    "问题: 查询所有商品的平均价格\nSQL: SELECT AVG(price) AS avg_price FROM products;",
                    "问题: 统计每种状态订单的数量\nSQL: SELECT status, COUNT(*) AS cnt FROM orders GROUP BY status ORDER BY cnt DESC;"
            );
            case TOP_N -> List.of(
                    "问题: 查询订单金额最高的前10个用户\nSQL: SELECT u.name, SUM(o.amount) AS total_amount FROM users u JOIN orders o ON u.id = o.user_id GROUP BY u.id ORDER BY total_amount DESC LIMIT 10;",
                    "问题: 查询价格最高的5个商品\nSQL: SELECT name, price FROM products ORDER BY price DESC LIMIT 5;"
            );
            case TREND -> List.of(
                    "问题: 统计每月的订单数量趋势\nSQL: SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS order_count FROM orders GROUP BY month ORDER BY month;",
                    "问题: 查询每天的销售额变化\nSQL: SELECT DATE(created_at) AS day, SUM(amount) AS daily_sales FROM orders GROUP BY day ORDER BY day;"
            );
            case JOIN -> List.of(
                    "问题: 查询用户张三的所有订单\nSQL: SELECT o.* FROM orders o JOIN users u ON o.user_id = u.id WHERE u.name = '张三';",
                    "问题: 查询每个用户购买的商品数量\nSQL: SELECT u.name, COUNT(oi.id) AS item_count FROM users u JOIN orders o ON u.id = o.user_id JOIN order_items oi ON o.id = oi.order_id GROUP BY u.id;"
            );
            case SUBQUERY -> List.of(
                    "问题: 查询订单金额超过平均金额的用户\nSQL: SELECT u.name FROM users u JOIN orders o ON u.id = o.user_id GROUP BY u.id HAVING SUM(o.amount) > (SELECT AVG(amount) FROM orders);",
                    "问题: 查询年龄最大的用户\nSQL: SELECT * FROM users WHERE age = (SELECT MAX(age) FROM users);"
            );
            case FUZZY -> List.of(
                    "问题: 查询名字包含'张'的用户\nSQL: SELECT * FROM users WHERE name LIKE '%张%';",
                    "问题: 查询商品名类似'手机'的商品\nSQL: SELECT * FROM products WHERE name LIKE '%手机%';"
            );
            case WHERE_FILTER -> List.of(
                    "问题: 查询年龄大于25岁的用户\nSQL: SELECT * FROM users WHERE age > 25;",
                    "问题: 查询状态为已完成的订单\nSQL: SELECT * FROM orders WHERE status = '已完成';"
            );
            case SIMPLE_SELECT -> List.of(
                    "问题: 查询所有用户\nSQL: SELECT * FROM users;",
                    "问题: 查询所有商品名称和价格\nSQL: SELECT name, price FROM products;"
            );
        };
    }
}
