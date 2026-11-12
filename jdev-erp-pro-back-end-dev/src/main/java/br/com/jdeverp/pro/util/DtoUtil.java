package br.com.jdeverp.pro.util;

import br.com.jdeverp.pro.model.Usuario;

public class DtoUtil {

	/**
	 * Nome do usuário para os DTOs: vem da pessoa ligada ao usuário
	 * (usuario.clienteFuncionario.pessoa.nome).
	 *
	 * O usuário precisa estar carregado na sessão atual. O usuário logado
	 * (UsuarioLogadoService) é carregado no JwtFilter, fora da sessão da
	 * requisição: busque-o de novo no repository antes de converter.
	 *
	 * @param usuario
	 * @return nome da pessoa, ou null se o usuário ou a pessoa não foram informados
	 */
	public static String nomeUsuario(Usuario usuario) {

		if (usuario == null || usuario.getClienteFuncionario() == null
				|| usuario.getClienteFuncionario().getPessoa() == null) {
			return null;
		}

		return usuario.getClienteFuncionario().getPessoa().getNome();
	}

}
