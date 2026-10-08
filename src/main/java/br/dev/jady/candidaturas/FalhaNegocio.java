package br.dev.jady.candidaturas;

final class FalhaNegocio extends RuntimeException {
    private static final long serialVersionUID = 1L;
    final int status;
    FalhaNegocio(int status, String mensagem) { super(mensagem); this.status = status; }
}
