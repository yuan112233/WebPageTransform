# WebPageTransform

一个自动转换网页格式的JAVA程序，把原本PC机上的网页转换成适合于用手机浏览的样式。

语言：JAVA

htmlDomTree包中包含了实现程序核心功能的代码，共包含三个类，其中HtmlDomTree为使用者需要调用的类，创建一个该类的实例即可实现对于网页布局的转换。

程序中用到了htmlcleaner和cssparser两个类库。

工程中还提供了两种测试方法：

方法1：以普通JAVA程序的方式运行程序，把转换后的网页输出到本地，参考main包下的MainTest类，这里还使用了JUnit作为测试工具。

方法2：实现一个Servlet，并把工程部署到Tomcat中，直接在浏览器中看到转换结果，具体参考test包下的Test类。在实际应用中应该主要采用这种方式。
