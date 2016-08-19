package mqtt.entity;

import java.util.List;
import mqtt.entity.MsgRep;
import mqtt.entity.MsgRepExample;
import org.apache.ibatis.annotations.Param;

public interface MsgRepMapper {
    int countByExample(MsgRepExample example);

    int deleteByExample(MsgRepExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(MsgRep record);

    int insertSelective(MsgRep record);

    List<MsgRep> selectByExampleWithBLOBs(MsgRepExample example);

    List<MsgRep> selectByExample(MsgRepExample example);

    MsgRep selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") MsgRep record, @Param("example") MsgRepExample example);

    int updateByExampleWithBLOBs(@Param("record") MsgRep record, @Param("example") MsgRepExample example);

    int updateByExample(@Param("record") MsgRep record, @Param("example") MsgRepExample example);

    int updateByPrimaryKeySelective(MsgRep record);

    int updateByPrimaryKeyWithBLOBs(MsgRep record);

    int updateByPrimaryKey(MsgRep record);
}