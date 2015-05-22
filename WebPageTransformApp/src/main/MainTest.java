package main;

import htmlDomTree.HtmlDomTree;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

@SuppressWarnings("unused")
public class MainTest {

	@Test
	public void test() throws MalformedURLException {
		String index="F:/Riviera/本科毕设/Test/ly";
		String bufIndex="F:/Riviera/本科毕设/Test/ly";
		String inputFile="setting.html";	
		String outputFile="output.html";
		//URL url=new URL("http://www.w3school.com.cn");

		HtmlDomTree tree;
		try {
			tree=new HtmlDomTree(index,inputFile,bufIndex,320);
			//tree = new HtmlDomTree(url,bufIndex,320);	
			tree.transform();						//转换页面布局
			PrintWriter pw = new PrintWriter(bufIndex+"/"+outputFile,tree.getCharset());
			tree.output(pw);						//生成转换后的网页并显示
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
