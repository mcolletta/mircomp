def grammarText = getClass().getResourceAsStream("mirgrams/melchordsym.mirgram").getText()
def gr = new MirGram(grammarText, "score", 3)
List<Integer> cods = []
def rand = new Random(123456789L)
(1..1000).each {
	cods << rand.nextInt(256)
}
def result = gr.translate(cods)