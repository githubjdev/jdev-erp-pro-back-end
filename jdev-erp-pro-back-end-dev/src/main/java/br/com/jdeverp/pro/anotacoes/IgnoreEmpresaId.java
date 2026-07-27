package br.com.jdeverp.pro.anotacoes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface IgnoreEmpresaId {

	boolean ignorar() default true;
	
	String motivo() default "";
	
}
