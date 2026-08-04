package br.com.jdeverp.pro.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

public class PostgreSQLFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		 SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
		 
		 
		    BasicType<String> stringType =
		            functionContributions.getTypeConfiguration()
		                    .getBasicTypeRegistry()
		                    .resolve(StandardBasicTypes.STRING);
		    
		    
		    BasicType<Double> doubleType =
		            functionContributions.getTypeConfiguration()
		                    .getBasicTypeRegistry()
		                    .resolve(StandardBasicTypes.DOUBLE);

		    registry.namedDescriptorBuilder("unaccent")
		            .setInvariantType(stringType)
		            .setExactArgumentCount(1)
		            .register();

		    registry.namedDescriptorBuilder("similarity")
		            .setInvariantType(doubleType)
		            .setExactArgumentCount(2)
		            .register();

		    registry.namedDescriptorBuilder("word_similarity")
		            .setInvariantType(doubleType)
		            .setExactArgumentCount(2)
		            .register();

		    registry.namedDescriptorBuilder("strict_word_similarity")
		            .setInvariantType(doubleType)
		            .setExactArgumentCount(2)
		            .register();
	}

}
