package com.nlp2sql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlp2sql.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
