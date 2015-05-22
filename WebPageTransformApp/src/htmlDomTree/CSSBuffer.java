/*
 * CSSBuffer.java
 *
 * 完成时间：2014.4.9
 * 编码人员：Riviera@BUPT
 * */

package htmlDomTree;

//import java.io.*;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * 包括对于CSS选择器以及选择器内属性的增、删、改、查等操作。
 * 用于临时存储修改后的CSS信息
 * @author Riviera
 *
 */
class CSSBuffer 
{
	/**
	 * 保存CSS信息的双层Map
	 */
	TreeMap<String,TreeMap<String,String>> css;

	public CSSBuffer()
	{
		css=new TreeMap<String,TreeMap<String,String>>();
	}
	
	/**
	 * 判断某选择器是否已存在
	 * @param selectorName
	 * @return
	 */
	public boolean hasSelector(String selectorName)
	{
		return !(css.get(selectorName)==null);
	}
	
	/**
	 * 新增一个选择器
	 * @param selectorName
	 */
	public void addSelector(String selectorName)
	{
		if(!hasSelector(selectorName))
		{
			TreeMap<String,String> att=new TreeMap<String,String>();
			css.put(selectorName,att);
		}
	}
	
	/**
	 * 在指定选择器内加入一个属性
	 * @param selectorName
	 * @param attName
	 * @param attValue
	 */
	public void addAtt(String selectorName,String attName,String attValue)
	{
		//addSelector(selectorName);
		css.get(selectorName).put(attName,attValue);
	}
	
	/**
	 * 输出全部CSS信息，以一个字符串的形式返回
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public String output()
	{
		String cssString=new String("");
		
		Iterator<Entry<String,TreeMap<String,String>>> iIt = css.entrySet().iterator();
		while(iIt.hasNext())
		{
			Entry iEntry=(Entry)iIt.next();
			String selector=(String)iEntry.getKey();
			cssString+=(selector+"\r\n{\r\n");
			@SuppressWarnings("unchecked")
			TreeMap<String,String> attMap=(TreeMap<String,String>)iEntry.getValue();
			Iterator<Entry<String,String>> jIt = attMap.entrySet().iterator();
			while(jIt.hasNext())
			{
				Entry jEntry=(Entry)jIt.next();
				String attName=(String)jEntry.getKey();
				String attValue=(String)jEntry.getValue();
				cssString+=("\t"+attName+":"+attValue+";\r\n");
			}
			cssString+="}\r\n";
		}		
		return cssString;
	}

}
