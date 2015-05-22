/*
 * CSSInfo.java
 * 
 * 完成时间：2014.4.9
 * 编码人员：Riviera@BUPT
 * 
 * 最后修改：2014.4.30
 * 修改人员：Riviera@BUPT
 * 修改内容：修正了CSS选择器匹配算法的BUG
 */

package htmlDomTree;

import java.io.*;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.net.*;

import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;

import com.steadystate.css.parser.CSSOMParser;

/**
 * 在CSSPaser的基础上实现了查询接口
 * @author Riviera
 *
 */
class CSSInfo 
{
	TreeMap<String,CSSStyleSheet> css;	//CSS信息
	String charset;						//使用字符集
	boolean local;						//CSS文件是否位于本地
	
	/**
	 * 构造函数
	 * @param Charset
	 * @param Local
	 */
	public CSSInfo(String Charset,boolean Local)
	{
		css=new TreeMap<String,CSSStyleSheet>();
		charset=Charset;
		local=Local;
	}
	
	/**
	 * 加入一个新的CSS
	 * @param cssHref	CSS文件的文件名，作为映射用的key
	 * @param URIPath	CSS文件的路径
	 */
	public void add(String cssHref,String URIPath)
	{
		css.put(cssHref,readCssFile(URIPath));
		//outputCss(cssHref);
	}
	
	/**
	 * 获取当前CSS表的数目
	 * @return
	 */
	public int size()
	{
		return css.size();
	}
	
	/**
	 * 在某个CSS表项内找到特定属性的值
	 * @param element			结点的名称
	 * @param selectorType		选择器类型（"#"、"."、""三种取值）
	 * @param selector			选择器名称（即结点中id或class属性的值）
	 * @param attName			需要寻找的CSS属性的名称
	 * @param fatherSelector	父选择器
	 * @return
	 */
	public String getAttValue(String element,String selectorType,String selector,String attName,String fatherSelector)
	{
		String value="";	//找到的属性的值
		int maxWeight=0;	//找到的最大权重
		Iterator<Entry<String,CSSStyleSheet>> it = css.entrySet().iterator();
		while(it.hasNext())
		{
			@SuppressWarnings("rawtypes")
			Entry entry=(Entry)it.next();
		    CSSStyleSheet cssStyleSheet=(CSSStyleSheet)entry.getValue();	
		    
		    CSSRuleList cssrules = cssStyleSheet.getCssRules();	
	        for (int i=0;i<cssrules.getLength();i++)
	        { 
	        	CSSRule rule = cssrules.item(i);  
	        	boolean flag=false;
	            if (rule instanceof CSSStyleRule)
	            {
	            	CSSStyleRule cssrule = (CSSStyleRule) rule;
	            	CSSStyleDeclaration styles=cssrule.getStyle();
	            	String[] selectorArray=cssrule.getSelectorText().split(",");
	            	for(int j=0;j<selectorArray.length && !flag;j++)
	            	{
	            		int pos=0;
	            		int weight=0;
	            		String curSelector=selectorArray[j];
	            		while(pos<selectorArray[j].length())
	            		{
	            			if(curSelector.charAt(pos)=='#')
	            				weight+=100;
	            			else if(curSelector.charAt(pos)=='.')
	            				weight+=10;
	            			else if(curSelector.charAt(pos)!=' ' && (pos==0 || curSelector.charAt(pos-1)==' '))
	            				weight++;
	            			pos++;
	            		}

	            		if(matchSelector(element,selectorType,selector,curSelector,fatherSelector))
	            		{
	            			for(int k=0;k<styles.getLength();k++)
	            			{
	            				if(styles.item(k).contains(attName) && weight>=maxWeight)
	            				{
	            					value=styles.getPropertyValue(styles.item(k));
	            					maxWeight=weight;
	            					flag=true;
	            				}
	            			}
	            		}
	            	}
	            }
	        }
		}
		return value;
	}
	
	/**
	 * 判断一个CSS选择器是否能选择到某个结点的id或class值
	 * @param element			结点的名称
	 * @param type				选择器类型（"#"、"."、""三种取值）
	 * @param value				选择器名称（即结点中id或class属性的值）	
	 * @param cssSelector		CSS表中的选择器，需要在此选择器中寻找前三个参数
	 * @param fatherSelector	父选择器
	 * @return					匹配成功返回true，不成功返回false
	 */
	boolean matchSelector(String element,String type,String value,String cssSelector,String fatherSelector)
	{
		String[] curArray=cssSelector.split(" ");
		String[] fatherArray=fatherSelector.split(" ");
		int i=0,j=0;
		for(;i<curArray.length-1;i++)
		{
			if(curArray[i].equals(" "))
				continue;
			if(curArray[i].length()>=1 && curArray[i].charAt(0)=='*')
				curArray[i]=curArray[i].substring(1);
			while(j<fatherArray.length && fatherArray[j].indexOf(curArray[i])==-1)
				j++;
			if(j==fatherArray.length)
				return false;
		}
		
		while(curArray[i].equals(" "))
			i--;
		String selector=curArray[i];
		if(!type.equals("") && selector.indexOf(element+type+value)!=-1)
			return true;
		else if(selector.indexOf('#')==-1 && selector.indexOf('.')==-1 && selector.indexOf(element)!=-1)
			return true;
		else if(!value.equals("") && selector.indexOf(value)!=-1)
			return true;
		else
			return false;
	}

	/**
	 * 转换CSS的编码格式
	 * @param oldURI	原CSS文件路径
	 * @param newURI	新CSS文件路径
	 * @param oldEncode	原CSS文件编码
	 * @param newEncode	新CSS文件编码
	 * @throws IOException
	 */
	public void transCssEncode(String oldURI,String newURI,String oldEncode,String newEncode) throws IOException
	{
		BufferedReader buf=null;
		if(oldURI.substring(0,5).equals("http:"))
			buf=new BufferedReader(new InputStreamReader(new URL(oldURI).openStream(),oldEncode));
		else
			buf=new BufferedReader(new InputStreamReader(new FileInputStream(oldURI),oldEncode));
	    OutputStreamWriter pw = new OutputStreamWriter(new FileOutputStream(newURI));
	    String str = null;
	    while((str = buf.readLine()) != null)
	    {
	    	String outStr = null;
	    	outStr = new String(str.getBytes(newEncode),newEncode);
	    	pw.write(outStr);
	    	pw.write("\r\n");
	    }
	    buf.close();
	    pw.close();
	}
		
	/**
	 * 读取单个CSS文件中的信息，并返回一个包含这些信息的CSSStyleSheet对象
	 * @param URIPath
	 * @return
	 */
	public CSSStyleSheet readCssFile(String URIPath)
	{				
		CSSOMParser cssparser = new CSSOMParser();   
	    CSSStyleSheet cssStyleSheet = null;   
	    try 
	    {
	    	cssStyleSheet= cssparser.parseStyleSheet(new InputSource(URIPath),null,null);
		}catch (IOException e) 
		{
			System.out.println("解析css文件异常:" + e);
		}
	    return cssStyleSheet;
	}

	/**
	 * 从字符串格式的CSS属性值中读取到实际的数值
	 * 可以读取以像素（px）、整体宽度百分比（%）以及相对字体大小（em）三种单位表示的数值，并全部转换为像素单位存储后返回
	 * value参数可被认为是函数的返回值
	 * @param valueStr	原始的CSS属性
	 * @param value		从原始CSS属性中得到的实际数值，可能有多个，以像素为单位
	 * @param num		需要读取到的数值的个数
	 * @param width		当前宽度，以像素为单位，用来计算以整体宽度百分比表示的数值的实际值
	 */
	public void readValue(String valueStr,int[] value,int num,int width)
	{
		double[] valueF={0,0,0,0};
		int i=0,j=0;
		int state=0;
		int count=1;
		while(j<valueStr.length() && i<num)
		{
			char ch=valueStr.charAt(j);
			switch(state)
			{
			case 0:
				if(ch=='0')
					i++;
				else if(ch=='a')
					break;
				else if(ch>='1' && ch<='9')
				{
					state=1;
					j--;
				}
				break;
			case 1:
				if(ch>='0' && ch<='9')
					value[i]=value[i]*10+ch-'0';
				else if(ch=='.')
				{
					state=2;
					count=1;
				}
				else if(ch=='p')
					state=3;
				else if(ch=='%')
					state=4;
				else if(ch=='e')
					state=5;
				break;
			case 2:
				if(ch>='1' && ch<='9')
				{
					value[i]=value[i]*10+ch-'0';
					count*=10;
				}
				else
				{
					valueF[i]=(double)value[i]/count;
					if(ch=='p')
						state=3;
					else if(ch=='%')
						state=4;
					else if(ch=='e')
						state=5;
				}
				break;
			case 3:
				j++;
				i++;
				state=0;
				break;
			case 4:
				value[i]=value[i]*width/100;
				i++;
				state=0;
				break;
			case 5:
				value[i]=(int)Math.ceil(valueF[i]*16);
				i++;
				state=0;
				break;
			default:break;
			}
			j++;
		}
		
		for(;i<num;i++)
			value[i]=value[0];
	}

	/**
	 * 输出单个CSS中的信息
	 * @param cssHref	CSS文件名（TreeMap中的键值）
	 */
	public void outputCss(String cssHref)
	{
		CSSStyleSheet cssStyleSheet=css.get(cssHref);
		if(cssStyleSheet!=null)
		{
			CSSRuleList cssrules = cssStyleSheet.getCssRules();	
	        for (int i = 0; i < cssrules.getLength(); i++)
	        { 
	        	CSSRule rule = cssrules.item(i);  
	            if (rule instanceof CSSStyleRule)
	            {
	            	CSSStyleRule cssrule = (CSSStyleRule) rule;
	            	System.out.println(cssrule.getSelectorText());
	            	System.out.println("{");
	            	CSSStyleDeclaration styles=cssrule.getStyle();
	            	for(int j=0,n=styles.getLength();j<n;j++)
	            		System.out.println("\t"+styles.item(j)+":"+styles.getPropertyValue(styles.item(j))+";");
	            	System.out.println("}");
	            }
	            else if (rule instanceof CSSImportRule)
	            {
	            	 CSSImportRule cssrule = (CSSImportRule) rule;  
	            	 System.out.println(cssrule.getHref());
	    		}
	        }
		}
	}	

}
