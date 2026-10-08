param([string]$BaseUrl = 'http://127.0.0.1:8081/api')
$ErrorActionPreference = 'Stop'
function Requisitar([string]$Metodo, [string]$Caminho, $Dados = $null) {
    $taskParametros = @{ Uri = "$BaseUrl$Caminho"; Method = $Metodo; ContentType = 'application/json; charset=utf-8' }
    if ($null -ne $Dados) { $taskParametros.Body = [System.Text.Encoding]::UTF8.GetBytes(($Dados | ConvertTo-Json -Depth 5)) }
    Invoke-RestMethod @taskParametros
}
$taskSufixo = [guid]::NewGuid().ToString('N').Substring(0,8)
$taskEmpresa = Requisitar POST '/empresas' @{ nome = "Empresa Exemplo $taskSufixo" }
$taskVaga = Requisitar POST '/vagas' @{ empresaId = $taskEmpresa.id; titulo = 'Estágio Java'; descricao = 'Vaga fictícia para demonstração'; link = 'https://example.com/vaga' }
$taskCandidatura = Requisitar POST '/candidaturas' @{ vagaId = $taskVaga.id; dataEnvio = [datetime]::UtcNow.AddDays(-1).ToString('yyyy-MM-dd'); observacoes = 'Demonstração' }
$taskEntrevista = Requisitar PATCH "/candidaturas/$($taskCandidatura.id)/status" @{ status = 'ENTREVISTA'; entrevistaEm = [datetimeoffset]::UtcNow.AddDays(1).ToString('o') }
Requisitar GET "/candidaturas/$($taskCandidatura.id)" | ConvertTo-Json -Depth 5
