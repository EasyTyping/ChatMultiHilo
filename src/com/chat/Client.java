package com.chat;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Cliente del chat Multihilo
 * 
 * Hay que configurar la IP del HOST. 
 * Contiene la Clase interna Paquete (DTO), los
 * objetos de esta clase transportar�n los datos
 * 
 * @author Jose Bejarano
 * @since 2018
 */


@SuppressWarnings("serial")
public class Client extends JFrame{
	
	//Componentes GUI
	private JPanel panel,  abajo, datos_conexion;
	static protected JTextArea conversation; 
	private static	JTextField write; 
	private static String nick;
	static private JComboBox<String> ips;
	private JButton enviar;
	static private String ip_selected;
	
	//Puertos e IP del servidor	
	static final String HOST="192.168.1.2";
	static  final int PORTIN=1111;
	static  final int PORTOUT= 1212;

	
	@SuppressWarnings("resource")
	public static void  main(String args[]){
		Client cliente=new Client();
		try {					
			ServerSocket sk_listening = new ServerSocket(PORTIN);
			while(true){				
				Socket sk= sk_listening.accept();
				/*Cuando llega una conexion creo un hilo y le paso como parametro el socket, 
				 * dejando libre la escucha por si entran nuevas conexiones*/
				cliente.new HilosEntrada(sk).start();
			}
		}	catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Constructor de la clase que me crear� la GUI
	Client(){
	
		nick= JOptionPane.showInputDialog("Introduce tu nick");
		/***** PANEL PRINCIPAL ****/
		panel=(JPanel) this.getContentPane();
	
		setTitle ("CLIENTE CHAT");
		setSize(400, 500);
		//Esta instrucci�n me centra la ventana sea cual sea la resolucion
		setLocationRelativeTo(null);	
		setResizable(false);//Evito que se pueda maximizar
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//Pongo el  layout en null para mover libremente los componentes 
		setLayout(null);
		
		/**  Campos ips & nick  ****/
		datos_conexion = new JPanel();
		//Combo de las ips de los usuarios conectados
		ips= new JComboBox<String>();
		//---------------------------
		JLabel etiqueta_nick=new JLabel("nick: ");	
		JLabel name=new JLabel(nick);	
		JLabel etiqueta_ip=new JLabel("Online: ");
		//---------------------------
		datos_conexion.setBounds(40, 20, 300,50);
		datos_conexion.add(etiqueta_nick);
		datos_conexion.add(name);
		datos_conexion.add(etiqueta_ip);
		datos_conexion.add(ips);
		panel.add(datos_conexion);
		
		/******* CAMPO CONVERSACI�N *****
		 *   Muestra la conversaci�n */
		conversation = new JTextArea(); 
		conversation.setEditable(false);
		conversation.setFont(new Font("Arial",Font.PLAIN,  12));
		conversation.setBounds(40,60, 300, 300 ); 
		panel.add(conversation);
				
		/********* PANEL ABAJO **********/
		abajo=new JPanel();	
		abajo.setBounds(40,360, 300, 75 );

		//Campo para escribir el mensaje
		write = new JTextField (20);
		//Genero un padding 
		write.setMargin(new Insets(5,5,5,5));
		/*Otra opci�n ser�a: write.setBorder(BorderFactory.createCompoundBorder(
		        write.getBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5)));*/
		write.setFont(new Font("Arial",Font.PLAIN,  12));
		abajo.add(write);			
		
		//Boton  enviar		
		enviar= new JButton("Enviar");
		abajo.add(enviar);
		panel.add(abajo);	
		
		//El setVisible debe ir siempre al final
		setVisible(true);
		//Evento y Escuchador boton enviar
		Evento_enviar eboton=new Evento_enviar();
		enviar.addActionListener(eboton);			
		//Evento de ventana
		addWindowListener(new ClienteOnline());
	}	

	
	/* Evento que se encarga de enviar los datos */
	private class Evento_enviar implements  ActionListener{
		public void actionPerformed(ActionEvent e) {
			//Creo un hilo cada vez que se pulsa el boton, evitando los posibles retardos
			new HilosSalida().start();
		}	
	}
	
//------------------------------INNER CLASSES--------------------------------------	
	
	/*Clase que crea un hilo de la clase OnOffLine
	 *  en funci�n de si se abre la ventana o se cierra */
class  ClienteOnline extends WindowAdapter{		
		public void windowOpened(WindowEvent e){ new OnOffLine().start(); }
		public void windowClosing(WindowEvent e){  new OnOffLine().start(); }
	}

/* Clase (hilo) que se encarga de notificar al servidor cuando nos conectamos
 * o nos desconectamos. Delegando en un hilo la operaci�n mejoro notablemente 
 * la carga de la lista de conectados y el cierre de la aplicaci�n. Adem�s prevengo que el 
 * cliente no se quede esperando en caso de que el servidor se haya cerrado antes*/
class OnOffLine extends Thread {

	public void run(){
			try {
				Socket sk_out = new Socket(HOST, PORTOUT);
				ObjectOutputStream  flow_out = new ObjectOutputStream(sk_out.getOutputStream());
				/*El segundo par�metro es false para indicar en el server que
				nos estamos conectando por primera vez, o lo que es lo mismo, que es falso que
				que estabamos online*/
				flow_out.writeObject(new Paquete(nick, false));			
				//Cerramos las conexiones
				if(sk_out!=null)sk_out.close();
				if(flow_out!=null) flow_out.close();
				
			} catch (IOException e1) {	
				e1.printStackTrace();
			}	
	}
}

class HilosSalida extends Thread{
		
		public void run(){
			ip_selected= (String) ips.getSelectedItem();
			try {
				//Realizamos la conexion
				Socket sk_out=new Socket(HOST, PORTOUT);
				 //Guardamos los datos en el DTO
				Paquete pack_out=new Paquete(nick, write.getText(), true, ip_selected) ;
				//Creamos el flujo y enviamos el DTO
				ObjectOutputStream  flow_out=new ObjectOutputStream(sk_out.getOutputStream());
				 flow_out.writeObject(pack_out);
				 conversation.append(nick +": "+ write.getText()+ "\n");
				 write.setText("");
				 //Cerramos la conexion
				 if(sk_out!=null) sk_out.close();
				 if(flow_out!=null) flow_out.close();
			 
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	
}

class HilosEntrada extends Thread{
	
	 private Socket sk_in;
	 
	HilosEntrada(Socket sk){ this.sk_in=sk;}
	
	public void run(){

		try {				
				ObjectInputStream flow_in= new ObjectInputStream(sk_in.getInputStream());		
				Paquete pack_in= (Paquete) flow_in.readObject();
				//Si recibo un paquete de alguien ya conectado, lo escribo en la conversaci�n
				if(pack_in.getOnline()==true)
						conversation.append(pack_in.getNick()+": "+ pack_in.getMsg()+ "\n"); 
				//Si recibo un paquete de un nuevo conectado
				else{
					//Consigo la lista de nuevos conectados, con el que mando el mensaje incluido
					List<String> lista_conectados= pack_in.getListaOnline();
					//Vaciamos el combo box antes de volver a llenarlo con la nueva lista
					ips.removeAllItems();
					//Cargo las ips de los conectados en el JComboBox
					lista_conectados.forEach(ip -> {  ips.addItem(ip); }) ;//Es lo mismo que: for(String ip:  lista_conectados){  ips.addItem(ip); }	
		       }
				sk_in.close();	
				flow_in.close();
			}	 catch (IOException|ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	}

}//----------------------------------FIN DE LA CLASE CLIENTE---------------------------------------------------



/**
 * DTO - Para transporatar los datos al servidor
 * 
 * @apunte he incluido el DTO dentro del propio cliente para poder exportar un Runnable Class File,
 * que ser� ejecutable sin incluir m�s ficheros, y as�, poder ejecutar el cliente en multiples equipos de la red
 * sin m�s tr�mites.. 
// * El DTO es usado tambi�n por la Clase Server, por lo que para que esta clase funcione hay 
 * que dejar una copia de Client.java en el mismo paquete  
 * 
 * @author Jose Bejarano  
 * */

//Serializado
 class Paquete implements Serializable{

	private static final long serialVersionUID = 1L;
	private String msg, ip, nick;
	private boolean online;
	private List<String> listaOnline;
	
	Paquete(){}
	
	//Contructor para enviar datos
	public Paquete( String nick, String msg, boolean online, String ip){
		this.ip = ip;
		this.nick=nick;
		this.msg=msg;
		this.online=online;
	}	
	
	/*Constructor del paquete cuando va a avisar que se ha conectado 
	 * un nuevo usuario o se ha desconectado alguno */
	public Paquete( String nick, boolean online) {
		this.nick = nick;
		this.online = online;
	}
	
	//GETTERS Y SETTERS
	public  List<String> getListaOnline() {
		return listaOnline;
	}
	public  void setListaOnline(List<String> listaOnline) {
		this.listaOnline = listaOnline;
	}
	
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getNick() {
		return nick;
	}
	public void setNick(String nick) {
		this.nick = nick;
	}

	public boolean getOnline() {
		return online;
	}	
	public void setOnline(boolean online) {
		this.online = online;
	}
	
}





