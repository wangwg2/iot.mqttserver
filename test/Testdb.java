import mqtt.db.DBSessionFactory;
import mqtt.entity.MsgRepExample;
import mqtt.entity.UserExample;

import org.apache.ibatis.session.SqlSession;


public class Testdb {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final  String queryMsg="mqtt.entity.MsgRepMapper.selectByExample";
		
		 SqlSession session=DBSessionFactory.getSqlSession();
	       
		 MsgRepExample example=new MsgRepExample();
	        
	        example.createCriteria().andMessageidEqualTo(1);
	      System.out.println(  session.selectOne(queryMsg, example));
	        session.close();
	}

}
