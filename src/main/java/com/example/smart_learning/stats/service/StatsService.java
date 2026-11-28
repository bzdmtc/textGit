package com.example.smart_learning.stats.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.stats.dto.DashboardDto;
import com.example.smart_learning.stats.dto.HotCourseDto;
import com.example.smart_learning.stats.dto.NamedCountDto;
import com.example.smart_learning.stats.dto.SummaryDto;
import com.example.smart_learning.stats.dto.TrendPointDto;
import com.example.smart_learning.stats.repository.StatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Master
public class StatsService {

    /** 数据库存 UTC；经营日报按北京时间自然日切分。 */
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private final StatsRepository statsRepository;

    public StatsService(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDto dashboard(int days) {
        int range = days <= 0 ? 14 : Math.min(days, 90);
        LocalDate today = LocalDate.now(BEIJING);
        LocalDate startDate = today.minusDays(range - 1L);
        LocalDateTime start = toUtc(startDate);
        LocalDateTime end = toUtc(today.plusDays(1));
        LocalDateTime todayStart = toUtc(today);
        LocalDateTime tomorrowStart = toUtc(today.plusDays(1));

        Object[] orders = statsRepository.orderTotals(todayStart, tomorrowStart);
        Object[] users = statsRepository.userTotals(todayStart, tomorrowStart);
        Object[] enrollments = statsRepository.enrollmentTotals(todayStart, tomorrowStart);

        long totalOrders = asLong(orders[0]);
        long paidOrders = asLong(orders[1]);
        BigDecimal totalSales = asDecimal(orders[2]);
        long todayOrders = asLong(orders[3]);
        BigDecimal todaySales = asDecimal(orders[4]);
        long totalUsers = asLong(users[0]);
        long todayUsers = asLong(users[1]);
        long totalEnrollments = asLong(enrollments[0]);
        long todayEnrollments = asLong(enrollments[1]);
        BigDecimal avgOrderAmount = paidOrders == 0 ? BigDecimal.ZERO
                : totalSales.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);

        SummaryDto summary = new SummaryDto(todaySales, todayOrders, todayUsers, todayEnrollments,
                totalSales, totalOrders, paidOrders, totalUsers, totalEnrollments,
                statsRepository.pendingRefunds(), avgOrderAmount);

        return new DashboardDto(range, today, summary,
                buildTrend(startDate, range, start, end),
                hotCourses(start, end),
                orderStatus(start, end));
    }

    private List<TrendPointDto> buildTrend(LocalDate startDate, int range, LocalDateTime start,
                                           LocalDateTime end) {
        Map<LocalDate, BigDecimal> sales = new HashMap<>();
        Map<LocalDate, Long> orders = new HashMap<>();
        for (Object[] row : statsRepository.ordersByDay(start, end)) {
            LocalDate date = toLocalDate(row[0]);
            orders.put(date, asLong(row[1]));
            sales.put(date, asDecimal(row[2]));
        }
        Map<LocalDate, Long> users = new HashMap<>();
        for (Object[] row : statsRepository.usersByDay(start, end)) {
            users.put(toLocalDate(row[0]), asLong(row[1]));
        }
        Map<LocalDate, Long> enrollments = new HashMap<>();
        for (Object[] row : statsRepository.enrollmentsByDay(start, end)) {
            enrollments.put(toLocalDate(row[0]), asLong(row[1]));
        }

        List<TrendPointDto> points = new ArrayList<>(range);
        for (int i = 0; i < range; i++) {
            LocalDate date = startDate.plusDays(i);
            points.add(new TrendPointDto(date.toString(),
                    sales.getOrDefault(date, BigDecimal.ZERO),
                    orders.getOrDefault(date, 0L),
                    users.getOrDefault(date, 0L),
                    enrollments.getOrDefault(date, 0L)));
        }
        return points;
    }

    private List<HotCourseDto> hotCourses(LocalDateTime start, LocalDateTime end) {
        return statsRepository.hotCourses(start, end).stream()
                .map(row -> new HotCourseDto(asLong(row[0]), (String) row[1], (String) row[2],
                        asLong(row[3]), asDecimal(row[4])))
                .toList();
    }

    private List<NamedCountDto> orderStatus(LocalDateTime start, LocalDateTime end) {
        return statsRepository.orderStatusCounts(start, end).stream()
                .map(row -> new NamedCountDto((String) row[0], asLong(row[1])))
                .toList();
    }

    private LocalDateTime toUtc(LocalDate date) {
        return date.atStartOfDay(BEIJING).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(value.toString());
    }

    private long asLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private BigDecimal asDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }
}
