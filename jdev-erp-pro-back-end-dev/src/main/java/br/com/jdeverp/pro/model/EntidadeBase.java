package br.com.jdeverp.pro.model;

import org.hibernate.proxy.HibernateProxy;

/**
 * Base das entidades: {@code equals} e {@code hashCode} pelo id, sem tocar em
 * relacionamento nenhum.
 *
 * <ul>
 * <li>Funcionam com o proxy LAZY do Hibernate sem carregá-lo, mesmo fora da
 * sessão: do proxy só se leem a classe mapeada e o id.</li>
 * <li>Entidade nova (id nulo) só é igual a ela mesma.</li>
 * <li>O {@code hashCode} é fixo por classe: não muda quando o id é gerado no
 * save, e a entidade continua sendo achada num {@code HashSet}.</li>
 * </ul>
 *
 * Os métodos são {@code final} para valerem também no proxy (uma subclasse da
 * entidade): se o proxy pudesse sobrescrevê-los, ele carregaria a entidade
 * antes de comparar.
 *
 * Não leva {@code @MappedSuperclass}: não tem coluna, e cada entidade continua
 * com o seu {@code @Id} e a sua sequence.
 */
public abstract class EntidadeBase {

	public abstract Long getId();

	@Override
	public final boolean equals(Object outro) {
		if (this == outro) {
			return true;
		}

		if (outro == null || classeDaEntidade(this) != classeDaEntidade(outro)) {
			return false;
		}

		Long id = getId();
		return id != null && id.equals(((EntidadeBase) outro).getId());
	}

	@Override
	public final int hashCode() {
		return classeDaEntidade(this).hashCode();
	}

	/**
	 * Classe da entidade, também para proxy não carregado e sem sessão aberta
	 * ({@code Hibernate.getClassLazy} exige sessão). Vale porque o projeto não
	 * usa herança entre entidades: a classe mapeada do proxy é a classe real.
	 */
	private static Class<?> classeDaEntidade(Object objeto) {
		return objeto instanceof HibernateProxy proxy
				? proxy.getHibernateLazyInitializer().getPersistentClass()
				: objeto.getClass();
	}

}
