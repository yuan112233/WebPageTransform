package test;

/*
 * 测试方法：
 * 
 * 1、部署项目，启动Tomcat
 * 2、在浏览器地址栏输入http://localhost:8080/WebPageTransformApp/Test
 * */

import htmlDomTree.HtmlDomTree;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("unused")
public class Test extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7218504709720949176L;

	/**
	 * 
	 */

	/**
	 * Constructor of the object.
	 */
	public Test() {
		super();
	}

	/**
	 * Destruction of the servlet. <br>
	 */
	public void destroy() {
		super.destroy(); // Just puts "destroy" string in log
		// Put your code here
	}

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html");
		
		String index="F:/Riviera/apache-tomcat-7.0.57/webapps/WebPageTransformApp/ly";
			//原网页所在路径（应在项目文件夹内）
		String bufIndex="F:/Riviera/apache-tomcat-7.0.57/webapps/WebPageTransformApp";
			//输出文件所在路径（必须为项目的根目录）
		String inputFile="setting.html";
			//原网页文件名
		//URL url=new URL("http://www.w3school.com.cn");
		
		//在地址栏输入localhost:8080/WebPageTransformApp/test 启动转换并显示

		HtmlDomTree tree;
		try {
			tree=new HtmlDomTree(index,inputFile,bufIndex,320);
			//tree = new HtmlDomTree(url,bufIndex,320);	
			response.setCharacterEncoding(tree.getCharset());
			tree.transform();						//转换页面布局
			PrintWriter pw = response.getWriter();	//选择直接在浏览器页面上显示转换后的网页
			tree.output(pw);						//生成转换后的网页并显示
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		out.println("<HTML>");
		out.println("  <HEAD><TITLE>A Servlet</TITLE></HEAD>");
		out.println("  <BODY>");
		out.print("    This is ");
		out.print(this.getClass());
		out.println(", using the GET method");
		out.println("  </BODY>");
		out.println("</HTML>");
		out.flush();
		out.close();
		*/
	}

	/**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		out.println("<HTML>");
		out.println("  <HEAD><TITLE>A Servlet</TITLE></HEAD>");
		out.println("  <BODY>");
		out.print("    This is ");
		out.print(this.getClass());
		out.println(", using the POST method");
		out.println("  </BODY>");
		out.println("</HTML>");
		out.flush();
		out.close();
	}

	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException if an error occurs
	 */
	public void init() throws ServletException {
		// Put your code here
	}

}
