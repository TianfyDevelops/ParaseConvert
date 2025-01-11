## byte数组转对象 对象转byte数组
1.通过反射+注解的形式，快速将一个byte数组转换为对象，将对象转换为byte数组
2.支持的基本数据类型+ArrayList
## 使用方式
1.定义Bean，为基本数据类型属性字段添加BaseFieldAnnotation注解，并指定order,order用来指定该属性在类中的顺序

2.定义ArrayList属性添加ConvertArray注解，并指定size集合的长度

3.通过ParserConvert.parseBytes2Bean或ParserConvert.parseBean2Bytes在对象和数组之间转换

