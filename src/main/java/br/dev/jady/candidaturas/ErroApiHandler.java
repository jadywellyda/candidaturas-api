package br.dev.jady.candidaturas;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ErroApiHandler {
    @ExceptionHandler(FalhaNegocio.class)
    ResponseEntity<ProblemDetail> negocio(FalhaNegocio erro) { return problema(erro.status, erro.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException erro) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revise os campos informados.");
        detalhe.setProperty("erros", erro.getBindingResult().getFieldErrors().stream()
                .map(c -> c.getField() + ": " + c.getDefaultMessage()).toList());
        return ResponseEntity.badRequest().body(detalhe);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> formato(Exception erro) { return problema(400, "JSON, data, identificador ou parâmetro inválido."); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integridade(Exception erro) { return problema(409, "Registro duplicado ou vinculado a outro registro."); }
    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> concorrencia(Exception erro) { return problema(409, "Registro alterado por outra operação. Consulte e tente novamente."); }
    private ResponseEntity<ProblemDetail> problema(int status, String mensagem) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), mensagem));
    }
}
