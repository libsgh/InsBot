package com.ins.bot;


import java.io.BufferedReader;
import java.io.StringReader;

import com.jfinal.template.Env;
import com.jfinal.template.TemplateException;
import com.jfinal.template.ext.directive.CallDirective;
import com.jfinal.template.io.CharWriter;
import com.jfinal.template.io.FastStringWriter;
import com.jfinal.template.io.Writer;
import com.jfinal.template.stat.Scope;
import com.jfinal.template.stat.ast.Define;

public class CompressDirective extends CallDirective {
	
	public void exec(Env env, Scope scope, Writer writer) {
		Object funcNameValue = funcNameExpr.eval(scope);
		if (funcNameValue == null) {
			if (nullSafe) {
				return ;
			}
			throw new TemplateException("模板函数名为 null", location);
		}
		
		if (!(funcNameValue instanceof String)) {
			throw new TemplateException("模板函数名必须是字符串", location);
		}
		
		Define func = env.getFunction(funcNameValue.toString());
		
		if (func == null) {
			if (nullSafe) {
				return ;
			}
			throw new TemplateException("模板函数未找到 : " + funcNameValue, location);
		}
		
		
		// -------------------------------------------------------------
		
		CharWriter charWriter = new CharWriter(64);
		FastStringWriter fsw = new FastStringWriter();
		charWriter.init(fsw);
		try {
			func.call(env, scope, paraExpr, charWriter);
		} finally {
			charWriter.close();
		}
		
		
		// -------------------------------------------------------------
		
		String content = fsw.toString();
		fsw.close();
		
		try (BufferedReader br = new BufferedReader(new StringReader(content))) {
			String line;
			while ((line=br.readLine()) != null) {
				fsw.append(line.trim());
			}
			write(writer, fsw.toString());
		} catch (Exception e) {
			throw new TemplateException(e.getMessage(), location);
		}
	}
	
}
