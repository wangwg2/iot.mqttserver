package mqtt.db;

import java.io.InputStream;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class DBSessionFactory {

	static SqlSessionFactory sessionFactory;
	
	
	public   static  void  init(){
		 String resource = "conf.xml";
	        //使用类加载器加载mybatis的配置文件（它也加载关联的映射文件）
	        InputStream is = DBSessionFactory.class.getClassLoader().getResourceAsStream(resource);
	        //构建sqlSession的工厂
	         sessionFactory = new SqlSessionFactoryBuilder().build(is);
		
	}
	
	public  static  SqlSession getSqlSession(){
		if(sessionFactory==null){
			init();
		}
		
		if(sessionFactory!=null)
			return  sessionFactory.openSession();
		
		
		return null;
		
		
	}
	
	
}
