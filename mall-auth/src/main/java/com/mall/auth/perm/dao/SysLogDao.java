package com.mall.auth.perm.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.auth.perm.entity.SysLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysLogDao extends BaseMapper<SysLogEntity> {

    @Select("SELECT COUNT(*) FROM sys_log")
    long countAll();

    @Select("SELECT COUNT(*) FROM sys_log WHERE status = 1")
    long countSuccess();

    @Select("SELECT COUNT(*) FROM sys_log WHERE status = 0")
    long countFail();

    @Select("SELECT COUNT(DISTINCT user_id) FROM sys_log")
    long countDistinctUsers();

    @Select("SELECT COUNT(DISTINCT method) FROM sys_log")
    long countDistinctMethods();

    @Select("SELECT COUNT(DISTINCT ip) FROM sys_log")
    long countDistinctIps();

    @Select("SELECT COUNT(*) FROM sys_log WHERE create_time >= CURDATE()")
    long countToday();

    @Select("SELECT COUNT(*) FROM sys_log WHERE status = 0 AND create_time >= CURDATE()")
    long countTodayFail();

    /** 失败方法 TOP N（方法失败情况） */
    @Select("SELECT method, COUNT(*) AS failCount FROM sys_log WHERE status = 0 " +
            "GROUP BY method ORDER BY failCount DESC LIMIT #{limit}")
    List<Map<String, Object>> topFailMethods(@Param("limit") int limit);

    /** 操作最频繁的用户 TOP N */
    @Select("SELECT COALESCE(username, '未知') AS username, COUNT(*) AS cnt FROM sys_log " +
            "GROUP BY username ORDER BY cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> topUsers(@Param("limit") int limit);

    /** 按天统计（from 起每天的总请求与成功数） */
    @Select("SELECT DATE(create_time) AS day, COUNT(*) AS total, SUM(status = 1) AS success " +
            "FROM sys_log WHERE create_time >= #{from} GROUP BY DATE(create_time) ORDER BY day")
    List<Map<String, Object>> countByDay(@Param("from") String from);

    /** 请求方法分布 TOP N（饼图） */
    @Select("SELECT method, COUNT(*) AS cnt FROM sys_log " +
            "GROUP BY method ORDER BY cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> methodDist(@Param("limit") int limit);

    /** 今日按小时统计 */
    @Select("SELECT HOUR(create_time) AS hour, COUNT(*) AS cnt FROM sys_log " +
            "WHERE create_time >= CURDATE() GROUP BY HOUR(create_time) ORDER BY hour")
    List<Map<String, Object>> countByHourToday();
}
