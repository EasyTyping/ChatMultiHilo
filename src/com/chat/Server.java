package com.chat;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Servidor MultiHilo
 * 
 * @author Jose Bejarano
 * @version 1.0
 *	@since 2018
 *
 */

@SuppressWarnings("serial")
public class Server extends JFrame{
	
	static  private final int PORTIN= 1212, PORTOUT=1111;
	private static List<String> listSynConectados;

	private static  JTextArea textPane; 	
	private static Logger log; 
	
	Server(){	
				setTitle ("SERVIDOR - CHAT");
				setSize(400, 500);
				textPane = new JTextArea();
				add(textPane);
				setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				setVisible(true);
	}
	
	public static void main(String[] args) {		
		
		Server server= new Server();
		//Obtenemos el Log de la clase 
		log = LogManager.getLogger(Server.class);

		//Lista de Conectados
		List<String> listaConectados= new ArrayList<String>();
		/*Sincronizamos la lista. Dado que con ella podr�an operar varios hilos a la vez, 
		 * es necesario que est� sincronizada para evitar posibles inconsistencias en los datos */
		listSynConectados= Collections.synchronizedList(listaConectados);
		//Creamos la ventana 
//		control= new Ventana(); 
		
		try {			
			@SuppressWarnings("resource")
			ServerSocket sk_server= new ServerSocket(PORTIN);
			log.info ("\nServidor iniciado y a la escucha=> \nAddress: "
			+ sk_server.getInetAddress().getHostAddress()
				+ "\nPuerto: "+ sk_server.getLocalPort());			
			//Entramos en un bucle infinito 
			while(true){	
				//Esperamos a una nueva conexion y cuando llegue la almacenamos en un socket
				Socket sk= sk_server.accept();		
				/*Creamos un nuevo hilo y le pasamos como par�metro al constructor el sk recibido.
				 * El nuevo hilo se encagar�  del tratamiento de la conexi�n recibida*/
				server.new Tasks(sk).start();			
			}
		} catch (IOException e) {
				e.printStackTrace();
		}
	}
	
	/*
	 * Clase que hereda de Thread y que realizar� el reenv�o de mensajes
	 * y la actualizaci�n de la lista de nuevos clientes conectados y desconectados
	 */
	  class Tasks extends Thread{
		
		private Paquete packDatos;
		private ObjectOutputStream out;		
		private Socket sk_in, sk_out;
		
		Tasks(Socket sk_in){
			this.sk_in= sk_in;
			log.info("\nN�mero estimado de hilos en ejecuci�n: " + Thread.activeCount());
		}
	
		public void run(){
	
		try{		
					//_________________RECIBIENDO DATOS_____________________
					String ipRemota= sk_in.getInetAddress().getHostAddress();
					//Cuando llegue algo obtenemos el Flujo
					ObjectInputStream in= new ObjectInputStream(sk_in.getInputStream());
					//Obtenemos del flujo el paquete recibido 
					packDatos = (Paquete) in.readObject();
					
								
					//_______________ EL CLIENTE YA EST� ONLINE________________
					if((packDatos.getOnline()) == true){
							//Mostramos la conversacion
							textPane.append(packDatos.getNick()+ ": " + packDatos.getMsg()+"\n");
							
							//_________________ENVIANDO DATOS AL DESTINATARIO_________________
							sk_out= new Socket(packDatos.getIp(), PORTOUT);
							out= new ObjectOutputStream(sk_out.getOutputStream());
							//Escribimos el paquete en el flujo
							out.writeObject(packDatos);
							log.trace("\nEl hilo "+ Thread.currentThread()+ " acaba de enviar un mensaje");
							if(out!=null) out.close();
								
					//_________________NUEVO CLIENTE ONLINE o CLIENTE DESCONECTANDOSE__________________________
					}else{
							/*Si la ipRemota ya se encuentra en la lista significa que estamos cerrando la ventana
							 * ya que el evento de ventana que provoca que entremos en este else solo ocurre en dos ocasiones:
							 * al abrir la ventana y al cerrarla
							 */
							if(!listSynConectados.contains(ipRemota)){
									//Linea de control
									log.info("\nSe acaba de conectar=> " + packDatos.getNick() +"\nSu ip es=> "+ ipRemota);
									//Incluyendo en la lista la ip del ultimo conectado 
									listSynConectados.add(ipRemota);
							}else{
									log.info("\nSe acaba de desconectar=> " + packDatos.getNick() +"\nSu ip es=> "+ ipRemota);
									listSynConectados.remove(ipRemota);
							}
							//Guardo la lista en el paquete que sera enviado
							packDatos.setListaOnline(listSynConectados);
							//Envio el paquete a todos los conectados 
							for (String ip : listSynConectados) {	
										sk_out= new Socket(ip, PORTOUT);
										out= new ObjectOutputStream(sk_out.getOutputStream());						
										out.writeObject(packDatos);
										out.close();
							}					
					}
					//Cerramos las conexiones			
					if(sk_out!=null)	sk_out.close();						
					if(sk_in!=null)	sk_in.close();		
					if(sk_out!=null)	in.close();		
			
		} catch (IOException |ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}


