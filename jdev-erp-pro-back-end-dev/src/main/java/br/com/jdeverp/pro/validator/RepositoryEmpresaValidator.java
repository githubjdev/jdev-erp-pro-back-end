package br.com.jdeverp.pro.validator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.stereotype.Component;

import br.com.jdeverp.pro.anotacoes.IgnoreEmpresaId;

@Component
public class RepositoryEmpresaValidator implements SmartInitializingSingleton {
	
	private final ApplicationContext applicationContext;
	
	
	public RepositoryEmpresaValidator(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
	private List<Class<?>> getRepositories(){
		List<Class<?>> repositores = new ArrayList<Class<?>>();
		applicationContext.getBeansOfType(RepositoryFactoryInformation.class)
		                 .values().forEach(info -> repositores.add(info.getRepositoryInformation().getRepositoryInterface()));
	    return repositores;
	}

	@Override
	public void afterSingletonsInstantiated() {
		for (Class<?> interfaceRepository: getRepositories()) {
			
			/*
			 * Ignora o Repository inteiro
			 */
			
			if(interfaceRepository.isAnnotationPresent(IgnoreEmpresaId.class)) {
				continue;
			}
			
			for(Method method: interfaceRepository.getMethods()) {
				
				/*
				 * Ignora apenas este método que contém @IgnoreEmpresaId
				 */
				if(method.isAnnotationPresent(IgnoreEmpresaId.class)) {
					continue;
				}
				
				/*Ignora se for método da extensão*/
				if(!method.getDeclaringClass().equals(interfaceRepository)) {
					continue;
				}
				
				/* Verifica se tem @Query escrita no método */
				boolean queryPresent = method.isAnnotationPresent(Query.class);
				
				if (!queryPresent) {
					throw new IllegalStateException("O método: "+ method 
                            + " da interface: "+ interfaceRepository 
                            + " deve possuir Query escrita.");
				}
				
				Query query = method.getAnnotation(Query.class);
				
				String sql = query.value().toLowerCase();
				
				/*Continua se tiver empresa.id e está correto*/
				if (sql.contains("empresa.id") || sql.contains("empresa_id")) {
					continue;
				}
				
				
				throw new IllegalStateException("""

						====================================================================
						ERRO DE SEGURANÇA

						Repository.....: %s
						Método.........: %s

						A consulta abaixo NÃO possui filtro por empresa.

						%s

						Toda consulta deve possuir:

						empresa.id

						ou

						empresa_id

						Caso esta consulta realmente não necessite
						do filtro, utilize:

						@IgnoreEmpresaId

						Essa anotação pode ser usada para o Repository completo ou para métodos unicos.
						====================================================================

						""".formatted(interfaceRepository.getSimpleName(), method.getName(), query.value()));
				
			}
			
		}
	}

}
