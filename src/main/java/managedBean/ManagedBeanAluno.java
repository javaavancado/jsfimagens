package managedBean;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;

import br.com.jsfimagens.Aluno;
import dao.DaoGeneric;

@ViewScoped
@ManagedBean(name = "managedBeanAluno")
public class ManagedBeanAluno {

	private Aluno aluno = new Aluno();
	private DaoGeneric<Aluno> daoGeneric = new DaoGeneric<Aluno>();

	private Part arquivo;

	public String salvar() throws IOException {
		String miniImgBase64 = DatatypeConverter.printBase64Binary(getBytes(arquivo.getInputStream()));

		// Convertendo para byte[] usando lib apache
		byte[] imageBytes = Base64.getDecoder().decode(miniImgBase64);
		aluno.setFotoIconBase64Original(imageBytes);
		
		// Transformando em BufferedImage
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

        // Pega o tipo da imagem
        int type = bufferedImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : bufferedImage.getType();

        //largura e a altura
        int largura = Integer.parseInt("300");
        int altura = Integer.parseInt("180");

        // Cria a imagem em minitura
        BufferedImage resizedImage = new BufferedImage(largura, altura, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(bufferedImage, 0, 0, largura, altura, null);
        g.dispose();

        // Escrevendo novamente a imagem em tamanho menor
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String extensao = arquivo.getContentType().split("\\/")[1];
        ImageIO.write(resizedImage, extensao, baos);

        miniImgBase64 = "data:" + arquivo.getContentType() + ";base64,"
                + DatatypeConverter.printBase64Binary(baos.toByteArray());

        aluno.setFotoIconBase64(miniImgBase64);
        aluno.setExtensao(extensao);

		aluno = daoGeneric.merge(aluno);
		return "";
	}

	public Aluno getAluno() {
		return aluno;
	}

	public void setAluno(Aluno aluno) {
		this.aluno = aluno;
	}

	public DaoGeneric<Aluno> getDaoGeneric() {
		return daoGeneric;
	}

	public void setDaoGeneric(DaoGeneric<Aluno> daoGeneric) {
		this.daoGeneric = daoGeneric;
	}

	public Part getArquivo() {
		return arquivo;
	}

	public void setArquivo(Part arquivo) {
		this.arquivo = arquivo;
	}

	public byte[] getBytes(InputStream is) throws IOException {

		int len;
		int size = 1024;
		byte[] buf;

		if (is instanceof ByteArrayInputStream) {
			size = is.available();
			buf = new byte[size];
			len = is.read(buf, 0, size);
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];
			while ((len = is.read(buf, 0, size)) != -1)
				bos.write(buf, 0, len);
			buf = bos.toByteArray();
		}
		return buf;
	}
	
	public void download() throws IOException{
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		String fileDownloadId = params.get("fileDownloadId");
		
		Aluno aluno = daoGeneric.findAluno(Aluno.class, fileDownloadId);
		
		HttpServletResponse response = (HttpServletResponse) FacesContext.
				getCurrentInstance().getExternalContext()
				.getResponse();
		response.addHeader("Content-Disposition", "attachment; filename=download." + aluno.getExtensao());
		response.setContentType("application/octet-stream");
		response.setContentLength(aluno.getFotoIconBase64Original().length);
		response.getOutputStream().write(aluno.getFotoIconBase64Original());
		response.getOutputStream().flush();
		FacesContext.getCurrentInstance().responseComplete();
	}

}
