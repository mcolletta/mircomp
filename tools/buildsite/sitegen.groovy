
import java.nio.file.attribute.*
import java.nio.file.*
import groovy.lang.Binding
import groovy.text.GStringTemplateEngine


def f = new File('./site/site.template')
def engine = new GStringTemplateEngine()
def template = engine.createTemplate(f)
//println template.toString()

List<String> roots = []
Path start = Paths.get("./site/content")
Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
        throws IOException
    {
        roots << path
        println path
        if (!Files.isDirectory(path)) {
        	createPage(path, template)
        }
        return FileVisitResult.CONTINUE
    }
})

void createPage(path, template) {
	def content = path.toFile().getText()
	def binding = [content:content, path:path]
	def page = template.make(binding)
	//println page
	def fout = new File('./site/output/' + path.getFileName())
	fout.setText(page.toString())
	//fout << page
}
