/*
 * HTMLDomTree.java
 * 
 * 完成时间：2014.2.16
 * 编码人员：Riviera@BUPT
 * 
 * 最后修改：2015.5.22
 * 修改人员：Riviera@BUPT
 * 修改内容：更新注释格式
 */

package htmlDomTree;

import java.io.*;
import java.util.Map;
import java.util.regex.Pattern;
import java.net.*;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

/**
 * 使用htmlcleaner工具，分析原HTML网页，生成树形结构
 * @author Riviera
 *
 */
public class HtmlDomTree 
{
	TagNode root;						
	
	String index;						//网页所在的目录
	String bufIndex;					//存放输出文件的目录
	String doctype;						//网页开头的<!DOCTYPE>标签
	String charset;						//网页编码所用的字符集
	boolean local;						//网页是否存在于本地
	CSSInfo css;						//网页的CSS表
	CSSBuffer newCss;					//修改后的CSS信息
	int screenWidth;					//移动设备屏幕的宽度
	
	PrintWriter printWriter;			//用于生成转换后网页的输出文件
	boolean print;						//是否在控制台输出CSS信息
	
	
	/***************************************初始化***************************************/
	
	/*
	 * 构造HTML-DOM树
	 * 两种构造型
	 */
	/**
	 * 构造型1：调用本地文件
	 * @param inputIndex	源文件所在目录路径
	 * @param fileName		源文件文件名
	 * @param inputBufIndex	输出文件存储目录路径
	 * @param inputWidth	转换后的网页屏幕宽度
	 * @throws Exception
	 */
	public HtmlDomTree(String inputIndex,String fileName,String inputBufIndex,int inputWidth) throws Exception
	{
		index=inputIndex;
		bufIndex=inputBufIndex;
		local=true;
		screenWidth=inputWidth;
		File file=new File(index+"/"+fileName);	
		checkCharset(index+"/"+fileName);		//检测网页所用的字符集
		readDocType(index+"/"+fileName);		//读取网页开头的<!DOCTYPE>标签
		print=false;
		
		HtmlCleaner cleaner=new HtmlCleaner();						
		root=cleaner.clean(file,charset);		//构建HTML-DOM树
	}

	/**
	 * 构造型2：通过域名调用网络文件
	 * @param url			网络文件路径
	 * @param inputBufIndex	临时文件存储目录路径
	 * @param inputWidth	转换后的网页屏幕宽度
	 * @throws Exception
	 */
	public HtmlDomTree(URL url,String inputBufIndex,int inputWidth) throws Exception
	{
		index=url.toString();
		int i=7;
		while(i<index.length() && index.charAt(i)!='/')
			i++;
		if(i<index.length())
			index=index.substring(0,i);
		bufIndex=inputBufIndex;
		local=false;
		screenWidth=inputWidth;
		checkCharset(index);					//检测网页所用的字符集
		readDocType(url.toString());			//读取网页开头的<!DOCTYPE>标签
		print=false;
		
		HtmlCleaner cleaner=new HtmlCleaner();
		root=cleaner.clean(url,charset);		//构建HTML-DOM树
	}

	/**
	 * 读取网页开头的<!DOCTYPE>标签
	 * 这一部分不会被存入HTML-DOM树中，但在生成新网页时还需要保留，所以单独存储
	 * @param URIPath
	 * @throws Exception
	 */
	private void readDocType(String URIPath) throws Exception
	{
		Reader reader;
		if(local)
		{
			File file=new File(URIPath);
			reader=new InputStreamReader(new FileInputStream(file),charset);	
		}
		else
		{
			URL url=new URL(URIPath);
			reader=new InputStreamReader(new BufferedInputStream(url.openStream()),charset);
		}
		doctype="";
		int ch;
		while((ch=reader.read())!='>')
			doctype+=(char)ch;
		doctype+='>';
		reader.close();
	}
	
	/**
	 * 检查网页所用的字符集
	 * @param URIPath
	 * @throws Exception
	 */
	private void checkCharset(String URIPath) throws Exception//XPatherException
	{
		BufferedReader buf=null;
		if(local)
			buf=new BufferedReader(new InputStreamReader(new FileInputStream(new File(URIPath))));	
		else
			buf=new BufferedReader(new InputStreamReader(new BufferedInputStream(new URL(URIPath).openStream())));
		
		String strBuf=null;
		int rowCount=0;
		while(rowCount<100)
		{
			rowCount++;
			strBuf=buf.readLine();
			if(strBuf.length()>0 && strBuf.indexOf("<meta")!=-1 && strBuf.indexOf("charset")!=-1)
			{
				int i=strBuf.indexOf("charset");
				strBuf=strBuf.substring(i+8).toLowerCase();
				i=strBuf.indexOf("\"");
				while(i==0)
				{
					strBuf=strBuf.substring(1);
					i=strBuf.indexOf("\"");
				}
				charset=strBuf.substring(0,i);
				break;
			}
		}
		if(rowCount>=100)
			charset="utf-8";
		System.out.println("Charset:\t"+charset);
		buf.close();
	}
	/***************************************初始化***************************************/
	
	/*************************************网页样式转换************************************/
	
	/**
	 * 把某项特定属性的值设为绝对路径
	 * @param att
	 * @throws XPatherException
	 */
	private void setAbsolutePath(String att) throws XPatherException
	{
		Object []ns=root.evaluateXPath("//*[@" + att + "]");
		for(Object object:ns)
		{
			TagNode node=(TagNode)object;
			String path="";
			path=node.getAttributeByName(att);
			if(path.length()<5 || !path.substring(0,4).equals("http"))					
			{
				if(path.charAt(0)=='/' || index.charAt(index.length()-1)=='/')	//补足文件前的左斜线
					path=index+path;
				else 
					path=index+"/"+path;
			}
			node.setAttribute(att,path);
		}
	}
	
	/**
	 * 读取网页的CSS信息
	 * @throws IOException
	 * @throws XPatherException
	 */
	private void readCss() throws IOException,XPatherException
	{
		css=new CSSInfo(charset,local);
		
			
		//外部CSS
		//找到所有存储外部CSS信息的结点（利用HtmlCleaner的xpath）
		Object []ns=root.evaluateXPath("//link[@rel]");
		for(Object object:ns)
		{
			TagNode cssLinkNode=(TagNode)object;
			if(!cssLinkNode.getAttributeByName("rel").toLowerCase().equals("stylesheet"))
				continue;			
			String cssHref=cssLinkNode.getAttributeByName("href");
			String URIPath=cssHref;
				
			//把网络上的CSS文件保存到本地，并转换编码格式为ANSI
			if(!local || !(charset.equals("gb2312" )|| charset.equals("gbk") || charset.equals("ansi")))
			{
				System.out.println("CSS "+css.size()+" out:\t"+URIPath);
				cssHref="ANSI_"+css.size()+".css";
				css.transCssEncode(index+"/"+URIPath,bufIndex+"/"+cssHref,charset,"gbk");
				URIPath=bufIndex+"/"+cssHref;
				if(!(charset.equals("gb2312" )|| charset.equals("gbk") || charset.equals("ansi")))
					cssLinkNode.setAttribute("href",cssHref);
			}
			//cssLinkNode.setAttribute("href", URIPath);
			URIPath="file:///"+URIPath;
			
						
			css.add(cssHref,URIPath);	
			System.out.println("CSS "+css.size()+" out:\t"+URIPath);	
		}
			
		//内部CSS
		//找到全部style标签
		ns=root.evaluateXPath("//style");
		for(Object object:ns)
		{
			TagNode styleNode=(TagNode)object;
			String cssFile=css.size()+".css";
			String URIPath=bufIndex+"/ANSI_"+cssFile;
			OutputStreamWriter pw=new OutputStreamWriter(new FileOutputStream(URIPath));
			pw.write(styleNode.getText().toString());	//把style标签下的text信息写入临时CSS文件
			pw.close();
			URIPath="file:///"+URIPath;
			
			css.add(cssFile,URIPath);	
			System.out.println("CSS "+css.size()+" in:\t"+URIPath);
		}
	}

	/**
	 * 网页样式转换主函数
	 * @throws Exception
	 */
	public void transform() throws Exception
	{
		if(!local)
		{
			setAbsolutePath("href");
			setAbsolutePath("src");
		}
			
		readCss();
		newCss=new CSSBuffer();
			
		System.out.println("Start transform...");
		
		transformDLR(root,screenWidth,"",0);	
	}

	/**
	 * 后序遍历DOM树，根据结点的类型修改其属性或对应的CSS，实现网页布局的转换
	 * @param node				当前所处结点
	 * @param width				父节点元素的宽度
	 * @param fatherSelector	父选择器
	 * @param deep				当前深度
	 */
	private void transformDLR(TagNode node,int width,String fatherSelector,int deep)
	{		
		//建立新的CSS选择器
		String element=node.getName();	//标签名
		String selectorType="";			//选择器类型（"."、"#"或""）	
		String selectorName="";			//选择器名称
		String selector="";				//整体选择器（复选择器+上面三项的连接）
		String selectorId="";			//临时保存ID选择器
		String selectorClass="";		//临时保存CLASS选择器
		if(node.hasAttribute("class"))
		{
			selectorType=".";
			selectorName=node.getAttributeByName("class");
			selectorClass=fatherSelector+" "+element+selectorType+selectorName;
			selector=selectorClass;
		}
		if(node.hasAttribute("id"))
		{
			selectorType="#";
			selectorName=node.getAttributeByName("id");
			selectorId=fatherSelector+" "+element+selectorType+selectorName;
			selector=selectorId;
		}
		if(!node.hasAttribute("class") && !node.hasAttribute("id"))
		{
			selector=fatherSelector;
			if(selector.indexOf(element)!=-1)
				selector+=(" "+element);
		}
		

		//修改对应当前结点的CSS属性
		//由于一个结点可能同时包含id和class属性，所以需要分别处理
		if(!node.hasAttribute("class") && !node.hasAttribute("id"))		//无id或class属性的情况
			width=setCss(node,element,"","",width,fatherSelector,deep);
		if(!selectorClass.equals(""))		//处理类选择器
		{
			String[] Class=node.getAttributeByName("class").split(" ");
			for(int i=0;i<Class.length;i++)	//类选择器可能同时有多个，需要分别处理
				width=setCss(node,element,".",Class[i],width,fatherSelector,deep);
		}
		if(!selectorId.equals(""))			//处理id选择器
			width=setCss(node,element,"#",node.getAttributeByName("id"),width,fatherSelector,deep);
		
		//向下遍历DOM树，处理子节点
		TagNode []children=node.getChildTags();
		if(children.length!=0)
		{
			for(int i=0;i<children.length;i++)
				if(!node.getName().equals("ins"))		//忽略ins标签（这种标签下的内容一般为广告）
					transformDLR(children[i],width,selector,deep+1);
		}		
	}
	
	/**
	 * 设置新的CSS
	 * @param node				当前所处结点
	 * @param element			结点名
	 * @param selectorType		选择器类型
	 * @param selectorName		选择器名
	 * @param width				当前宽度
	 * @param fatherSelector	父选择器
	 * @param deep				当前深度
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private int setCss(TagNode node,String element,String selectorType,String selectorName,int width,String fatherSelector,int deep)
	{
		//更新宽度，在原有宽度的基础上减去左右内边界宽度、边框宽度和外边界宽度
		//边界值的写法有四项合在一起写和四项分开写两种，需要分别处理
		String borderStr;
		int []margin={0,0,0,0};
		int []border={0,0,0,0};
		int []padding={0,0,0,0};
		int []left={0};
		int []right={0};
		borderStr=css.getAttValue(element, selectorType, selectorName, "margin", fatherSelector);
		css.readValue(borderStr, margin, 4, width);
		borderStr=css.getAttValue(element, selectorType, selectorName, "margin-left", fatherSelector);
		css.readValue(borderStr, left, 1, width);
		borderStr=css.getAttValue(element, selectorType, selectorName, "margin-right", fatherSelector);
		css.readValue(borderStr, right, 1, width);
		if(left[0]!=0)
			margin[3]=left[0];
		if(right[0]!=0)
			margin[1]=right[0];
		borderStr=css.getAttValue(element, selectorType, selectorName, "border", fatherSelector);
		css.readValue(borderStr, border, 4, width);
		borderStr=css.getAttValue(element, selectorType, selectorName, "border-left", fatherSelector);
		css.readValue(borderStr, left, 1, width);
		borderStr=css.getAttValue(element, selectorType, selectorName, "border-right", fatherSelector);
		css.readValue(borderStr, right, 1, width);
		if(left[0]!=0)
			border[3]=left[0];
		if(right[0]!=0)
			border[1]=right[0];
		borderStr=css.getAttValue(element, selectorType, selectorName, "padding", fatherSelector);
		css.readValue(borderStr, padding, 4, width);
		borderStr=css.getAttValue(element, selectorType, selectorName, "padding-left", fatherSelector);
		css.readValue(borderStr, left, 1, width);
		borderStr=css.getAttValue(element, selectorType, selectorName, "padding-right", fatherSelector);
		css.readValue(borderStr, right, 1, width);
		if(left[0]!=0)
			padding[3]=left[0];
		if(right[0]!=0)
			padding[1]=right[0];
		int max=(int)Math.floor((double)width/200);
		for(int i=1;i<=3;i+=2)
		{
			if(margin[i]>max)
				margin[i]=max;
			if(border[i]>max)
				border[i]=max;
			if(padding[i]>max)
				padding[i]=max;
		}
		width-=(margin[1]+margin[3]+border[1]+border[3]+padding[1]+padding[3]);
		
		String selector=fatherSelector+" "+element+selectorType+selectorName;
		//根据标签类型对CSS进行相应的修改
		if(element.equals("html"))
			node.addAttribute("style", "width:"+String.valueOf(width)+"px;");
		
		else if(element.equals("body"))
		{
			if(node.hasAttribute("id"))
				selector="body#"+node.getAttributeByName("id");
			else if(node.hasAttribute("class"))
				selector="body."+node.getAttributeByName("class");
			else
				selector="body";
			newCss.addSelector(selector);
			newCss.addAtt(selector,"width",String.valueOf(width)+"px");
			String imgUrl=css.getAttValue(element, "", "", "background-image", "");
			if(!imgUrl.equals("") && imgUrl.indexOf("url")!=-1)
				newCss.addAtt(selector,"background-image",setCssUrl(imgUrl));
		}
		
		else if(element.equals("div"))
		{
			newCss.addSelector(selector);								//增加新的选择器
			//newCss.addAtt(selector,"width",String.valueOf(width)+"px");	
			newCss.addAtt(selector, "background-repeat", "no-repeat");
			String oldWidth=css.getAttValue(element, selectorType, selectorName, "width", fatherSelector);
			String oldHeight=css.getAttValue(element, selectorType, selectorName, "height", fatherSelector);
			//更新后的宽度和高度值直接作为属性写入节点内部，确保优先级为最高
			if(!(!oldWidth.equals("") && Pattern.compile("[0-9]*").matcher(oldWidth).matches() && Integer.valueOf(oldWidth)<width/3))
				if(!oldHeight.equals(""))
					node.addAttribute("style", "width:"+String.valueOf(width)+"px;"+"height:"+"auto");
				else
					node.addAttribute("style", "width:"+String.valueOf(width)+"px;");	//固定宽度
			newCss.addAtt(selector,"min-height",oldHeight);
			newCss.addAtt(selector,"text-align","left");							//文本左对齐
			newCss.addAtt(selector,"position","static");							//消除绝对定位
			newCss.addAtt(selector,"float","none");									//消除块内浮动
			newCss.addAtt(selector,"margin-left",String.valueOf(margin[3])+"px");	//调整块内边距和外边距为一个较小的数值
			newCss.addAtt(selector,"margin-right",String.valueOf(margin[1])+"px");
			newCss.addAtt(selector,"padding-left",String.valueOf(padding[3])+"px");
			newCss.addAtt(selector,"padding-right",String.valueOf(padding[1])+"px");
			String imgUrl=css.getAttValue(element, selectorType, selectorName, "background-image", fatherSelector);
			if(imgUrl.equals(""))
				imgUrl=css.getAttValue(element, selectorType, selectorName, "background", fatherSelector);
			if(!imgUrl.equals("") && imgUrl.indexOf("url")!=-1)			//修改背景图片路径为绝对路径，保证能够显示背景
				newCss.addAtt(selector,"background",setCssUrl(imgUrl));		
		}
		
		else if(element.equals("span"))
		{
			newCss.addSelector(selector);
			newCss.addAtt(selector,"max-width",String.valueOf(width)+"px");
			newCss.addAtt(selector,"text-align","left");
			newCss.addAtt(selector,"float","none");
			newCss.addAtt(selector,"margin-left",String.valueOf(margin[3])+"px");
			newCss.addAtt(selector,"margin-right",String.valueOf(margin[1])+"px");
			newCss.addAtt(selector,"padding-left",String.valueOf(padding[3])+"px");
			newCss.addAtt(selector,"padding-right",String.valueOf(padding[1])+"px");
			String imgUrl=css.getAttValue(element, selectorType, selectorName, "background-image", fatherSelector);
			if(imgUrl.equals(""))
				imgUrl=css.getAttValue(element, selectorType, selectorName, "background", fatherSelector);
			if(!imgUrl.equals("") && imgUrl.indexOf("url")!=-1)
				newCss.addAtt(selector,"background-image",setCssUrl(imgUrl));
		}

		else if(element.equals("ul") || element.equals("ol"))
		{
			newCss.addSelector(selector);
			newCss.addAtt(selector,"position","static");
			newCss.addAtt(selector,"background","none");
			if(!css.getAttValue(element, selectorType, selector, "width", fatherSelector).equals(""))
				newCss.addAtt(selector,"width",String.valueOf(width)+"px");
			if(margin[3]!=0)
				newCss.addAtt(selector,"margin-left",String.valueOf(margin[3])+"px");
			if(margin[1]!=0)
				newCss.addAtt(selector,"margin-right",String.valueOf(margin[1])+"px");
			newCss.addAtt(selector,"max-width",String.valueOf(width)+"px");
			String imgUrl=css.getAttValue(element, selectorType, selectorName, "background-image", fatherSelector);
			if(imgUrl.equals(""))
				imgUrl=css.getAttValue(element, selectorType, selectorName, "background", fatherSelector);
			if(!imgUrl.equals("") && imgUrl.indexOf("url")!=-1)
				newCss.addAtt(selector,"background-image",setCssUrl(imgUrl));
		}
		
		else if(element.equals("li"))
		{
			newCss.addSelector(selector);
			//newCss.addAtt(selector,"float","left");
			newCss.addAtt(selector,"display","inline");		//则把列表横向排列
			newCss.addAtt(selector,"list-style-type","none");	//取消表项前的图标样式
			newCss.addAtt(selector,"position","static");
			newCss.addAtt(selector,"max-width",String.valueOf(width)+"px");
		}
		
		else if((element.indexOf("h")==0 && element.length()==2) || element.equals("p") || element.equals("pre") || element.equals("dl") || element.equals("dd") || element.equals("dt"))
		{
			newCss.addSelector(selector);
			newCss.addAtt(selector,"margin-left",String.valueOf(margin[3])+"px");
			newCss.addAtt(selector,"margin-right",String.valueOf(margin[1])+"px");
			newCss.addAtt(selector,"padding-left",String.valueOf(padding[3])+"px");
			newCss.addAtt(selector,"padding-right",String.valueOf(padding[1])+"px");
			newCss.addAtt(selector,"max-width",String.valueOf(width)+"px");
			newCss.addAtt(selector,"text-indent","0.1em");
			newCss.addAtt(selector,"position","static");
			String height=css.getAttValue(element, selectorType, selectorName, "height", fatherSelector);
			if(!height.equals(""))
			{
				newCss.addAtt(selector,"height","auto");
				newCss.addAtt(selector,"min-height",height);
			}
		}
		
		else if(element.equals("a"))
		{
			if(node.getParent().getName().equals("li"))	//对于列表下的超链接标签，需要进行特殊处理		
			{
				newCss.addSelector(selector);
				newCss.addAtt(selector,"max-width",String.valueOf(width)+"px");
				if(node.getText().length()<20)
					newCss.addAtt(selector,"display","inline");
				newCss.addAtt(selector,"padding","0 0.25em");
			}
		}
		
		else if(element.equals("table"))
		{
			//newCss.addSelector(selector);
		}
		
		else if(element.equals("tr"))
		{
			TagNode[] children=node.getChildTags();
			int cLength=children.length;
			if(cLength>2)
			{
				for(int i=0;i<cLength;i+=2)
				{
					if(i%2==0)
					{
						TagNode newNode=new TagNode("tr");
						newNode.addChild(children[i]);
						node.replaceChild(children[i],newNode);
						if(i+1<cLength)
						{
							newNode.addChild(children[i+1]);
							node.removeChild(children[i+1]);
						}
							
					}
						
				}
			}
		}
		
		else if(element.equals("input"))
		{
			newCss.addSelector(selector);
			newCss.addAtt(selector,"min-height","30px");					//限制输入框的高度的最小值为一个较大值，方便点击
			newCss.addAtt(selector,"max-width",String.valueOf(width)+"px");	//限制输入框的最大宽度为当前块的宽度
		}
		
		else if(element.equals("img"))
		{
			newCss.addSelector(selector);
			newCss.addAtt(selector,"max-width",String.valueOf(width)+"px");	//限制图片的最大宽度为当前块的宽度
			newCss.addAtt(selector,"height","auto");						//设置高度为自动按比例调整，防止图片变形
			if(local)
			{
				String path=node.getAttributeByName("src");
				node.setAttribute("src", index.replaceAll(bufIndex+"/", "")+"/"+path);
			}
		}
		
		else if(element.equals("textarea"))
		{
			//newCss.addSelector(selector);
		}	
		
		return width;
	}
	
	/**
	 * 转变CSS中URL的格式，把相对地址转换成绝对地址
	 * @param imgUrl
	 * @return
	 */
	private String setCssUrl(String imgUrl)
	{
		int urlPos=imgUrl.indexOf("url(")+4;
		String sub1=imgUrl.substring(0,urlPos);
		while(imgUrl.charAt(urlPos)=='.')
			urlPos++;
		String sub2=imgUrl.substring(urlPos);
		return sub1+index+sub2;
	}
	/*************************************网页样式转换************************************/
	
	/****************************************输出****************************************/

	/**
	 * 生成转换后的新网页
	 * @param inputPrintWriter
	 */
	public void output(PrintWriter inputPrintWriter)
	{
		this.printWriter=inputPrintWriter;
		if(doctype!=null)
			printWriter.print(doctype+"\r");	//输出开头的<!DOCTYPE>标签
		outputDLR(root,0);						//先序遍历，输出各个结点的信息
		printWriter.close();
		System.out.println("Finish.");			//结束标记
	}

	/**
	 * 生成转换后的网页，输出其中的信息
	 * @param node
	 * @param deep
	 */
	private void outputDLR(TagNode node,int deep)
	{
		String name=node.getName();
		StringBuffer text=node.getText();
		
		/*
		int weight=Integer.parseInt(node.getAttributeByName("weight"));
		
		if((!node.hasAttribute("class") && !node.hasAttribute("id") && (weight/10000<1 && weight%10000<1)) && !name.equals("iframe"))
			return;*/		
		
		printWriter.print("<"+name);		//输出标签名
		Map<String,String> att=node.getAttributes();
		for(String attName:att.keySet())	//输出属性
			printWriter.print(" "+attName+"=\""+att.get(attName)+"\"");
		if(!sym(name))						//若为非对称标签，则需要输出结束符/（XHTML规定）
			printWriter.print(" /");
		printWriter.print(">\r\n");
		
		if(node.hasChildren())						//向下遍历，寻找子结点
		{
			TagNode []children=node.getChildTags();
			boolean isText=true;
			for(int i=0;i<children.length && isText;i++)
				if(!children[i].getName().equals("br"))
					isText=false;
			if(isText)
				printWriter.println(text);//输出结点下的文本
			
			for(int i=0;i<children.length;i++)
				outputDLR(children[i],deep+1);		//向下一层遍历
		}
		else
			printWriter.println(text);
		
		if(node.getName().equals("head"))	//在head标签结束前需要输出CSS修改信息
		{
			printWriter.println("<style>");
			printWriter.println(newCss.output());	//CSS修改信息
			printWriter.println("</style>");
		}
		
		if(sym(name))						//若为对称标签，则需要输出右侧标签
			printWriter.print("</"+name+">\r\n");
	}
	
	/**
	 * 判断一个标签是否为对称标签
	 * @param name	标签名
	 * @return		是对称标签返回true，不是则返回false
	 */
	private boolean sym(String name)
	{
		if(name.equals("img") || name.equals("input") || name.equals("meta") || name.equals("link"))
			return false;
		else
			return true;
	}
	/****************************************输出****************************************/

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}
}