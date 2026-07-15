import { useCallback, useEffect, useState } from 'react';
import {
  Bar, BarChart, CartesianGrid, Line, LineChart, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis
} from 'recharts';
import {
  Boxes, ChartNoAxesCombined, CircleUserRound, ClipboardList, LogOut,
  Menu, Package, Pencil, Plus, RefreshCw, ShoppingBag, ShoppingCart,
  CreditCard, ExternalLink, Printer, ReceiptText, Search, ShieldCheck, Tag, Trash2, Truck, UserRound, Users, X
} from 'lucide-react';
import './App.css';

const API = '/api';
const money = value => new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(Number(value || 0));
const date = value => value ? new Date(value).toLocaleString('es-CL', { dateStyle: 'short', timeStyle: 'short' }) : '—';
const blankProduct = { sku:'', productName:'', category:'', warehouseCode:'', price:'', description:'', imageUrl:'', initialQuantity:0, reorderLevel:0, active:true };
const DEFAULT_PRODUCT_IMAGE = '/product-placeholder.svg';
const productImage = value => value?.trim() || DEFAULT_PRODUCT_IMAGE;
const useDefaultImage = event => {
  if (!event.currentTarget.src.endsWith(DEFAULT_PRODUCT_IMAGE)) event.currentTarget.src = DEFAULT_PRODUCT_IMAGE;
};

async function api(path, options = {}) {
  const token = localStorage.getItem('smartlogix_token');
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: { 'Content-Type':'application/json', ...(token ? { Authorization:`Bearer ${token}` } : {}), ...(options.headers || {}) }
  });
  if (response.status === 401 && token) {
    localStorage.removeItem('smartlogix_token'); localStorage.removeItem('smartlogix_session');
    window.dispatchEvent(new Event('smartlogix-logout'));
  }
  if (!response.ok) {
    let message = `Error ${response.status}`;
    try { const body = await response.json(); message = body.message || body.error || message; } catch { /* sin body */ }
    throw new Error(message);
  }
  return response.status === 204 ? null : response.json();
}

function usePolling(callback, delay = 5000) {
  useEffect(() => { callback(); const id = setInterval(callback, delay); return () => clearInterval(id); }, [callback, delay]);
}

export default function App() {
  const [session, setSession] = useState(() => JSON.parse(localStorage.getItem('smartlogix_session') || 'null'));
  const [toast, setToast] = useState(null);
  useEffect(() => { const logout = () => setSession(null); window.addEventListener('smartlogix-logout', logout); return () => window.removeEventListener('smartlogix-logout', logout); }, []);
  const notify = (message, type='ok') => { setToast({message,type}); setTimeout(() => setToast(null), 4000); };
  const logout = () => { localStorage.removeItem('smartlogix_token'); localStorage.removeItem('smartlogix_session'); setSession(null); };
  return <>{toast && <div className={`toast ${toast.type}`}>{toast.message}</div>}{!session ? <Auth onLogin={setSession} notify={notify}/> : session.role === 'ROLE_ADMIN' ? <Admin session={session} logout={logout} notify={notify}/> : <Client session={session} setSession={setSession} logout={logout} notify={notify}/>}</>;
}

function Auth({onLogin, notify}) {
  const [register, setRegister] = useState(false);
  const [form, setForm] = useState({firstName:'',lastName:'',email:'',password:''});
  const [loading, setLoading] = useState(false);
  const submit = async e => {
    e.preventDefault(); setLoading(true);
    try {
      if (register) {
        const data = await api('/auth/register', {method:'POST', body:JSON.stringify(form)});
        notify(`${data.message} Cupón: ${data.welcomeCoupon}`); setRegister(false); setForm(f=>({...f,password:''}));
      } else {
        const data = await api('/auth/login', {method:'POST', body:JSON.stringify({credential:form.email,password:form.password})});
        const session = {userId:data.userId,firstName:data.firstName,lastName:data.lastName,username:data.username,email:data.email,role:data.role};
        localStorage.setItem('smartlogix_token', data.token); localStorage.setItem('smartlogix_session', JSON.stringify(session)); onLogin(session);
      }
    } catch (e) { notify(e.message,'error'); } finally { setLoading(false); }
  };
  return <main className="auth-page"><section className="auth-hero"><div className="logo-mark">SL</div><h1>SmartLogix</h1><p>Comercio, inventario y logística sincronizados en una sola plataforma.</p><div className="hero-badges"><span>Stock real</span><span>16 regiones</span><span>Backend seguro</span></div></section><form className="auth-card" onSubmit={submit}><p className="eyebrow">SMARTLOGIX PLATFORM</p><h2>{register?'Crear cuenta':'Bienvenido de vuelta'}</h2><p>{register?'Regístrate y recibe tu 10% de bienvenida.':'Ingresa con tu correo y contraseña.'}</p>{register && <div className="two"><label>Nombre<input required autoComplete="given-name" value={form.firstName} onChange={e=>setForm({...form,firstName:e.target.value})}/></label><label>Apellido<input required autoComplete="family-name" value={form.lastName} onChange={e=>setForm({...form,lastName:e.target.value})}/></label></div>}<label>Correo<input required type="email" autoComplete="email" value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></label><label>Contraseña<input required minLength="6" type="password" autoComplete={register?'new-password':'current-password'} value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/></label><button className="primary wide" disabled={loading}>{loading?'Procesando…':register?'Crear cuenta':'Iniciar sesión'}</button><button type="button" className="text-button" onClick={()=>setRegister(!register)}>{register?'Ya tengo cuenta':'Crear una cuenta nueva'}</button><div className="admin-hint"><b>Administrador</b><span>admin@smartlogix.com · admin1</span></div></form></main>;
}

function Shell({session, nav, active, setActive, logout, children}) {
  const [open,setOpen]=useState(false);
  return <div className="shell"><aside className={open?'open':''}><div className="side-brand"><div className="logo-mark small">SL</div><div><b>SmartLogix</b><span>{session.role==='ROLE_ADMIN'?'Administración':'Mi cuenta'}</span></div><button className="mobile-close" onClick={()=>setOpen(false)}><X/></button></div><nav>{nav.map(({id,label,icon:Icon,badge})=><button key={id} className={active===id?'active':''} onClick={()=>{setActive(id);setOpen(false)}}><Icon size={19}/><span>{label}</span>{badge>0&&<em>{badge}</em>}</button>)}</nav><div className="side-user"><CircleUserRound/><div><b>{session.firstName} {session.lastName}</b><span>{session.email}</span></div></div><button className="logout" onClick={logout}><LogOut size={18}/>Cerrar sesión</button></aside><main className="content"><header className="topbar"><button className="menu" onClick={()=>setOpen(true)}><Menu/></button><div><p>SMARTLOGIX</p><h1>{nav.find(n=>n.id===active)?.label}</h1></div><span className="role-pill">{session.role==='ROLE_ADMIN'?'ADMINISTRADOR':'CLIENTE'}</span></header>{children}</main></div>;
}

function Client({session,setSession,logout,notify}) {
  const [tab,setTab]=useState('home'); const [products,setProducts]=useState([]); const [orders,setOrders]=useState([]); const [regions,setRegions]=useState([]); const [coupon,setCoupon]=useState(null); const [cart,setCart]=useState([]); const [checkout,setCheckout]=useState(false); const [editing,setEditing]=useState(null);
  const load=useCallback(async()=>{try{const [p,o,r]=await Promise.all([api('/inventory/catalog'),api('/orders/my'),api('/shipments/regions')]);setProducts(p);setOrders(o);setRegions(r);setCoupon({available:false,code:''});}catch(e){notify(e.message,'error')}},[notify]);
  usePolling(load,5000);
  const add=p=>{if(!p.available){notify('Lo sentimos. En estos momentos este producto no tiene stock disponible.','error');return;}setCart(cur=>{const found=cur.find(x=>x.sku===p.sku);return found?cur.map(x=>x.sku===p.sku?{...x,quantity:x.quantity+1}:x):[...cur,{...p,quantity:1}]});notify('Producto agregado al carrito.');};
  const nav=[{id:'home',label:'Inicio',icon:ChartNoAxesCombined},{id:'store',label:'Tienda',icon:ShoppingBag},{id:'prices',label:'Comparar precios',icon:Search},{id:'orders',label:'Mis órdenes',icon:ClipboardList},{id:'routes',label:'Adaptador inteligente',icon:Truck},{id:'profile',label:'Mi perfil',icon:UserRound},{id:'cart',label:'Carrito',icon:ShoppingCart,badge:cart.reduce((a,x)=>a+x.quantity,0)}];
  return <Shell session={session} nav={nav} active={tab} setActive={setTab} logout={logout}>{tab==='home'&&<ClientHome session={session} orders={orders} coupon={coupon} go={setTab}/>} {tab==='store'&&<Store products={products} add={add}/>} {tab==='prices'&&<PriceFinder session={session} regions={regions} notify={notify} afterOrder={()=>{setTab('orders');load()}}/>} {tab==='cart'&&<Cart cart={cart} setCart={setCart} onCheckout={()=>setCheckout(true)}/>} {tab==='orders'&&<ClientOrders orders={orders} notify={notify} onEdit={setEditing} onCancel={async o=>{if(!confirm(`¿Cancelar ${o.orderNumber}?`))return;try{await api(`/orders/my/${o.orderNumber}`,{method:'DELETE'});notify('Orden cancelada y stock liberado.');load();}catch(e){notify(e.message,'error')}}}/>} {tab==='routes'&&<RouteAdapter regions={regions} notify={notify}/>} {tab==='profile'&&<Profile session={session} setSession={setSession} notify={notify} logout={logout}/>} {checkout&&<Checkout session={session} cart={cart} regions={regions} coupon={coupon} close={()=>setCheckout(false)} done={()=>{setCart([]);setCheckout(false);setTab('orders');load()}} notify={notify}/>} {editing&&<EditOrder order={editing} regions={regions} close={()=>setEditing(null)} done={()=>{setEditing(null);load()}} notify={notify}/>}</Shell>;
}

function ClientHome({session,orders,coupon,go}) { const pending=orders.filter(o=>o.status==='PENDING').length; return <><section className="welcome"><div><p className="eyebrow">HOLA, {session.firstName.toUpperCase()}</p><h2>Todo tu SmartLogix en un solo lugar.</h2><p>Compra, compara envíos y sigue tus órdenes en tiempo real.</p><button className="primary" onClick={()=>go('store')}>Explorar tienda</button></div><ShoppingBag size={90}/></section><section className="stats"><Stat icon={ClipboardList} label="Órdenes" value={orders.length}/><Stat icon={RefreshCw} label="Pendientes" value={pending}/><Stat icon={Tag} label="Cupón" value={coupon?.available?'10% disponible':'Utilizado'}/></section>{coupon?.available&&<section className="coupon-card"><div><Tag/><span>BIENVENIDA</span></div><strong>{coupon.code}</strong><p>10% de descuento en productos, válido una sola vez.</p></section>}<section className="panel"><div className="panel-head"><div><h3>Últimas órdenes</h3><p>Seguimiento actualizado automáticamente.</p></div><button className="secondary" onClick={()=>go('orders')}>Ver todas</button></div><OrderTable orders={orders.slice(0,5)}/></section></> }

function Store({products,add}) { return <section><div className="section-title"><div><h2>Catálogo</h2><p>Productos activos obtenidos directamente desde inventario.</p></div><span>{products.length} productos</span></div>{products.length===0?<Empty text="Aún no hay productos activos. El administrador puede agregarlos desde Inventario."/>:<div className="product-grid">{products.map(p=><article className="product" key={p.sku}><div className="product-image"><img src={productImage(p.imageUrl)} alt={p.name} onError={useDefaultImage}/></div><div className="product-body"><h3>{p.name}</h3><p>{p.description}</p><div><strong>{money(p.price)}</strong><button className="primary" disabled={!p.available} onClick={()=>add(p)}>{p.available?'Comprar':'Agotado'}</button></div></div></article>)}</div>}</section> }

function PriceFinder({session, regions, notify, afterOrder}) {
  const [query,setQuery]=useState('');
  const [data,setData]=useState(null);
  const [loading,setLoading]=useState(false);
  const [selected,setSelected]=useState(null);

  const search=async event=>{
    event.preventDefault();
    const clean=query.trim();
    if(clean.length<2){notify('Escribe un producto para buscar.','error');return;}
    setLoading(true);
    try{
      const result=await api(`/price-finder/search?query=${encodeURIComponent(clean)}&limit=24`);
      setData(result);
      notify('Resultados encontrados.');
    }catch(e){notify(e.message,'error')}
    finally{setLoading(false)}
  };

  return <section>
    <div className="section-title"><div><h2>Comparador de precios</h2></div><span>SmartLogix Price Finder</span></div>
    <form className="price-search" onSubmit={search}>
      <label>Busca cualquier producto
        <input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Ej: notebook gamer, audífonos bluetooth, monitor, celular..." />
      </label>
      <button className="primary" disabled={loading}>{loading?'Buscando…':'Buscar mejor precio'}</button>
    </form>

    {data&&<>
      <section className="panel price-summary">
        <div className="panel-head"><div><h3>Mejor precio encontrado</h3></div><span className="status active">{data.totalResults} resultados</span></div>
        <div className="best-price-card">
          <div><span>{data.bestOption.store}</span><h3>{data.bestOption.title}</h3><p>{data.bestOption.seller||'Proveedor externo'} · {data.bestOption.condition||'Nuevo'}</p></div>
          <strong>{money(data.bestOption.price)}</strong>
          <button className="primary" onClick={()=>setSelected(data.bestOption)}>Simular compra</button>
        </div>
        
      </section>

      <section className="panel">
        <div className="panel-head"><div><h3>Tiendas encontradas</h3></div></div>
        <div className="table-wrap"><table><thead><tr><th>Tienda</th><th>Producto</th><th>Precio</th><th>Estado</th><th>Fuente</th><th>Acción</th></tr></thead><tbody>{data.results.map((item,index)=><tr key={`${item.store}-${item.title}-${index}`}>
          <td><b>{item.store}</b><small>{item.seller}</small></td>
          <td><b>{item.title}</b>{item.externalUrl&&<a className="external-link" href={item.externalUrl} target="_blank" rel="noreferrer"><ExternalLink size={14}/> Ver referencia</a>}</td>
          <td><strong>{money(item.price)}</strong></td>
          <td>{item.condition||'Nuevo'}</td>
          <td>{item.source}</td>
          <td><button className="secondary" onClick={()=>setSelected(item)}>Simular compra</button></td>
        </tr>)}</tbody></table></div>
      </section>
    </>}

    {selected&&<SymbolicPurchaseModal session={session} option={selected} regions={regions} close={()=>setSelected(null)} notify={notify} done={()=>{setSelected(null);afterOrder();}}/>}
  </section>
}

function SymbolicPurchaseModal({session,option,regions,close,notify,done}) {
  const nextYear=new Date().getFullYear()+1;
  const [loading,setLoading]=useState(false);
  const [form,setForm]=useState({
    address:'Compra simbólica generada desde SmartLogix Price Finder',
    region:regions[0]||'Metropolitana de Santiago',
    cardHolderName:`${session.firstName} ${session.lastName}`,
    cardNumber:'',
    expiryMonth:'',
    expiryYear:String(nextYear),
    securityCode:'',
    installments:'1'
  });
  const submit=async event=>{
    event.preventDefault();
    if(!form.cardHolderName||!form.cardNumber||!form.expiryMonth||!form.expiryYear||!form.securityCode){notify('Completa los datos de la tarjeta ficticia.','error');return;}
    setLoading(true);
    try{
      const order=await api('/orders/symbolic-external',{method:'POST',body:JSON.stringify({
        customerName:`${session.firstName} ${session.lastName}`,
        externalStore:option.store,
        externalSeller:option.seller,
        externalProductName:option.title,
        externalPrice:option.price,
        currency:option.currency||'CLP',
        externalUrl:option.externalUrl,
        imageUrl:option.imageUrl,
        shippingAddress:form.address,
        shippingRegion:form.region,
        payment:{
          cardHolderName:form.cardHolderName,
          cardNumber:form.cardNumber,
          expiryMonth:Number(form.expiryMonth),
          expiryYear:Number(form.expiryYear),
          securityCode:form.securityCode,
          installments:Number(form.installments)
        }
      })});
      notify(`Compra simbólica registrada. Orden ${order.orderNumber} · Boleta ${order.receiptNumber}.`);
      done();
    }catch(e){notify(e.message,'error')}
    finally{setLoading(false)}
  };

  return <Modal title="Simular compra externa" close={close}>
    <form className="form-grid" onSubmit={submit}>
      <div className="full symbolic-purchase-card">
        <p className="eyebrow">COMPRA SIMBÓLICA</p>
        <h3>{option.title}</h3>
        <p>{option.store} · {option.seller}</p>
        <strong>{money(option.price)}</strong>
        <small>SmartLogix no realiza una compra real en esta tienda externa.</small>
      </div>
      <label className="full">Dirección simbólica
        <input required value={form.address} onChange={e=>setForm({...form,address:e.target.value})}/>
      </label>
      <label>Región
        <select value={form.region} onChange={e=>setForm({...form,region:e.target.value})}>{(regions.length?regions:['Metropolitana de Santiago']).map(r=><option key={r}>{r}</option>)}</select>
      </label>
      <label>Nombre tarjeta ficticia
        <input required autoComplete="cc-name" value={form.cardHolderName} onChange={e=>setForm({...form,cardHolderName:e.target.value})}/>
      </label>
      <label>Número ficticio
        <input required inputMode="numeric" autoComplete="cc-number" placeholder="4111111111111111" value={form.cardNumber} onChange={e=>setForm({...form,cardNumber:e.target.value.replace(/[^0-9 ]/g,'')})}/>
      </label>
      <label>Mes
        <input required type="number" min="1" max="12" autoComplete="cc-exp-month" value={form.expiryMonth} onChange={e=>setForm({...form,expiryMonth:e.target.value})}/>
      </label>
      <label>Año
        <input required type="number" min={new Date().getFullYear()} autoComplete="cc-exp-year" value={form.expiryYear} onChange={e=>setForm({...form,expiryYear:e.target.value})}/>
      </label>
      <label>CVV ficticio
        <input required inputMode="numeric" maxLength="4" type="password" autoComplete="cc-csc" value={form.securityCode} onChange={e=>setForm({...form,securityCode:e.target.value.replace(/\D/g,'')})}/>
      </label>
      <label>Cuotas
        <select value={form.installments} onChange={e=>setForm({...form,installments:e.target.value})}>{[1,2,3,6,12].map(n=><option key={n} value={n}>{n}</option>)}</select>
      </label>
      <p className="full symbolic-note">El pago es académico y simbólico. No se procesan pagos externos, no se guarda CVV y no se compra en la tienda real.</p>
      <button className="primary" disabled={loading}>{loading?'Registrando…':'Registrar compra simbólica'}</button>
    </form>
  </Modal>
}

function Cart({cart,setCart,onCheckout}) { const total=cart.reduce((a,x)=>a+Number(x.price)*x.quantity,0); return <section className="panel"><div className="panel-head"><div><h3>Tu carrito</h3><p>El precio y el stock se validarán nuevamente en el backend.</p></div></div>{cart.length===0?<Empty text="Tu carrito está vacío."/>:<><div className="cart-list">{cart.map(x=><div className="cart-item" key={x.sku}><img src={productImage(x.imageUrl)} alt={x.name} onError={useDefaultImage}/><div><b>{x.name}</b><span>{money(x.price)} c/u</span></div><input aria-label="Cantidad" type="number" min="1" value={x.quantity} onChange={e=>setCart(c=>c.map(y=>y.sku===x.sku?{...y,quantity:Math.max(1,Number(e.target.value))}:y))}/><strong>{money(Number(x.price)*x.quantity)}</strong><button className="icon danger" onClick={()=>setCart(c=>c.filter(y=>y.sku!==x.sku))}><Trash2/></button></div>)}</div><div className="cart-total"><span>Subtotal referencial</span><strong>{money(total)}</strong></div><button className="primary" onClick={onCheckout}>Continuar al envío</button></>}</section> }

function Checkout({session,cart,regions,coupon,close,done,notify}) {
  const nextYear = new Date().getFullYear() + 1;
  const [form,setForm]=useState({
    address:'',
    region:'',
    type:'',
    coupon:coupon?.available?(coupon.code||''):'',
    cardHolderName:`${session.firstName} ${session.lastName}`,
    cardNumber:'',
    expiryMonth:'',
    expiryYear:String(nextYear),
    securityCode:'',
    installments:'1'
  });
  const [routes,setRoutes]=useState([]);
  const [acceptedRoute,setAcceptedRoute]=useState(null);
  const [loading,setLoading]=useState(false);
  const units=cart.reduce((a,x)=>a+x.quantity,0);
  const selectedRegion=form.region||regions[0]||'';

  useEffect(()=>{
    let active=true;
    api('/shipments/selections/current')
      .then(selection=>{
        if(!active)return;
        if(selection && selection.totalUnits===units){
          setAcceptedRoute(selection);
          setForm(current=>({...current,region:selection.region,type:selection.type}));
        }else{
          setAcceptedRoute(null);
        }
      })
      .catch(()=>{});
    return()=>{active=false};
  },[units]);

  const calculate=async()=>{
    try{
      setRoutes(await api('/shipments/recommendations',{
        method:'POST',
        body:JSON.stringify({region:selectedRegion,units})
      }));
    }catch(e){notify(e.message,'error')}
  };

  const acceptRoute=async route=>{
    try{
      const selection=await api('/shipments/selections',{
        method:'POST',
        body:JSON.stringify({region:selectedRegion,totalUnits:units,routeType:route.type})
      });
      setAcceptedRoute(selection);
      setForm(current=>({...current,region:selection.region,type:selection.type}));
      setRoutes([]);
      notify('Ruta aceptada y guardada correctamente.');
    }catch(e){notify(e.message,'error')}
  };

  const submit=async event=>{
    event.preventDefault();
    if(!form.address||!acceptedRoute){
      notify('Ingresa la dirección y acepta una alternativa de envío.','error');
      return;
    }
    if(!form.cardHolderName||!form.cardNumber||!form.expiryMonth||!form.expiryYear||!form.securityCode){
      notify('Completa todos los datos de la tarjeta ficticia.','error');
      return;
    }
    setLoading(true);
    try{
      const order = await api('/orders',{
        method:'POST',
        body:JSON.stringify({
          customerName:`${session.firstName} ${session.lastName}`,
          shippingAddress:form.address,
          shippingRegion:acceptedRoute.region,
          shippingType:acceptedRoute.type,
          routeSelectionId:acceptedRoute.selectionId,
          lines:cart.map(x=>({sku:x.sku,quantity:x.quantity})),
          couponCode:form.coupon||null,
          payment:{
            cardHolderName:form.cardHolderName,
            cardNumber:form.cardNumber,
            expiryMonth:Number(form.expiryMonth),
            expiryYear:Number(form.expiryYear),
            securityCode:form.securityCode,
            installments:Number(form.installments)
          }
        })
      });
      notify(`Pago aprobado. Orden ${order.orderNumber} y boleta ${order.receiptNumber} generadas.`);
      done();
    }catch(e){notify(e.message,'error')}
    finally{setLoading(false)}
  };

  return <Modal title="Finalizar compra y pagar" close={close}>
    <form onSubmit={submit}>
      <section className="checkout-section">
        <div className="checkout-title"><Truck/><div><h3>Dirección y envío</h3><p>La ruta aceptada queda guardada en el backend y se reutiliza al pagar.</p></div></div>
        <div className="form-grid">
          <label className="full">Dirección
            <input required autoComplete="street-address" value={form.address} onChange={e=>setForm({...form,address:e.target.value})} placeholder="Calle, número y comuna"/>
          </label>
          <label>Región
            <select value={acceptedRoute?.region||selectedRegion} disabled={Boolean(acceptedRoute)} onChange={e=>{setForm({...form,region:e.target.value});setRoutes([])}}>
              {regions.map(r=><option key={r}>{r}</option>)}
            </select>
          </label>
          <label>Cupón
            <input value={form.coupon} disabled={!coupon?.available} onChange={e=>setForm({...form,coupon:e.target.value.toUpperCase()})}/>
          </label>
        </div>

        {!acceptedRoute&&<>
          <button className="secondary" type="button" onClick={calculate}>Calcular las 3 alternativas</button>
          <div className="route-list">
            {routes.map(r=><article className="route-option" key={r.type}>
              <div><b>{routeLabel(r.type)}</b><span>{r.routeName} · {r.carrier}</span><small>{r.distanceKm} km · {r.estimatedDays} días · llega {r.estimatedDate}</small></div>
              <strong>{money(r.price)}</strong>
              <button className="primary accept-route" type="button" onClick={()=>acceptRoute(r)}>Aceptar ruta</button>
            </article>)}
          </div>
        </>}

        {acceptedRoute&&<article className="route-option selected accepted-route-summary">
          <div><b>{routeLabel(acceptedRoute.type)}</b><span>{acceptedRoute.routeName} · {acceptedRoute.carrier}</span><small>{acceptedRoute.distanceKm} km · {acceptedRoute.estimatedDays} días · llega {acceptedRoute.estimatedDate} · {acceptedRoute.routeCode}</small></div>
          <strong>{money(acceptedRoute.price)}</strong>
        </article>}
      </section>

      {acceptedRoute&&<section className="checkout-section payment-box">
        <div className="checkout-title"><CreditCard/><div><h3>Pago con tarjeta ficticia</h3><p>No se guarda el número completo ni el CVV. Tarjeta de prueba sugerida: 4111 1111 1111 1111.</p></div></div>
        <div className="form-grid">
          <label className="full">Nombre del titular
            <input required autoComplete="cc-name" value={form.cardHolderName} onChange={e=>setForm({...form,cardHolderName:e.target.value})}/>
          </label>
          <label className="full">Número de tarjeta
            <input required autoComplete="cc-number" inputMode="numeric" maxLength="23" placeholder="4111 1111 1111 1111" value={form.cardNumber} onChange={e=>setForm({...form,cardNumber:e.target.value.replace(/[^0-9 ]/g,'')})}/>
          </label>
          <label>Mes
            <select autoComplete="cc-exp-month" value={form.expiryMonth} onChange={e=>setForm({...form,expiryMonth:e.target.value})}>
              <option value="">MM</option>
              {Array.from({length:12},(_,i)=>i+1).map(m=><option key={m} value={m}>{String(m).padStart(2,'0')}</option>)}
            </select>
          </label>
          <label>Año
            <input required autoComplete="cc-exp-year" type="number" min={new Date().getFullYear()} value={form.expiryYear} onChange={e=>setForm({...form,expiryYear:e.target.value})}/>
          </label>
          <label>CVV
            <input required type="password" autoComplete="cc-csc" inputMode="numeric" maxLength={4} value={form.securityCode} onChange={e=>setForm({...form,securityCode:e.target.value.replace(/\D/g,'')})}/>
          </label>
          <label>Cuotas
            <select value={form.installments} onChange={e=>setForm({...form,installments:e.target.value})}>
              {[1,2,3,6,12].map(n=><option key={n} value={n}>{n} cuota{n>1?'s':''}</option>)}
            </select>
          </label>
        </div>
        <div className="secure-note">🔒 Pago académico validado y persistido por order-service; no se almacena el número completo ni el CVV.</div>
      </section>}

      {acceptedRoute&&<button className="primary wide pay-button" type="submit" disabled={loading}>
        {loading?'Procesando pago…':'Pagar y generar boleta electrónica'}
      </button>}
    </form>
  </Modal>
}

function RouteAdapter({regions,notify}) {
  const [region,setRegion]=useState('');
  const [units,setUnits]=useState(1);
  const [routes,setRoutes]=useState([]);
  const [acceptedRoute,setAcceptedRoute]=useState(null);
  const selectedRegion=region||regions[0]||'';

  useEffect(()=>{
    let active=true;
    api('/shipments/selections/current').then(selection=>{
      if(!active||!selection)return;
      setAcceptedRoute(selection);
      setRegion(selection.region);
      setUnits(selection.totalUnits);
    }).catch(()=>{});
    return()=>{active=false};
  },[]);

  const run=async()=>{
    try{
      setRoutes(await api('/shipments/recommendations',{
        method:'POST',
        body:JSON.stringify({region:selectedRegion,units:Number(units)})
      }));
    }catch(e){notify(e.message,'error')}
  };

  const accept=async route=>{
    try{
      const saved=await api('/shipments/selections',{
        method:'POST',
        body:JSON.stringify({region:selectedRegion,totalUnits:Number(units),routeType:route.type})
      });
      setAcceptedRoute(saved);
      notify('Ruta aceptada, guardada y disponible para el pago.');
    }catch(e){notify(e.message,'error')}
  };

  return <section className="panel"><div className="panel-head"><div><h3>Adaptador inteligente de envíos</h3><p>Compara costo, distancia y tiempo para las 16 regiones de Chile.</p></div></div><div className="route-controls"><label>Región<select value={selectedRegion} onChange={e=>setRegion(e.target.value)}>{regions.map(r=><option key={r}>{r}</option>)}</select></label><label>Unidades<input type="number" min="1" value={units} onChange={e=>setUnits(e.target.value)}/></label><button className="primary" onClick={run}>Calcular rutas</button></div><div className="route-cards">{routes.map(r=><article key={r.type} className={acceptedRoute?.type===r.type&&acceptedRoute?.region===selectedRegion&&acceptedRoute?.totalUnits===Number(units)?'selected':''}><span>{routeLabel(r.type)}</span><h3>{r.carrier}</h3><p>{r.routeName}</p><dl><div><dt>Precio</dt><dd>{money(r.price)}</dd></div><div><dt>Tiempo</dt><dd>{r.estimatedDays} días</dd></div><div><dt>Distancia</dt><dd>{r.distanceKm} km</dd></div><div><dt>Fecha</dt><dd>{r.estimatedDate}</dd></div></dl><button className="primary wide" type="button" onClick={()=>accept(r)}>{acceptedRoute?.type===r.type&&acceptedRoute?.region===selectedRegion&&acceptedRoute?.totalUnits===Number(units)?'Ruta aceptada':'Aceptar ruta'}</button></article>)}</div>{acceptedRoute&&<p className="secure-note">Ruta guardada: {routeLabel(acceptedRoute.type)} · {acceptedRoute.carrier} · {acceptedRoute.routeCode}</p>}</section>
}

function ClientOrders({orders,onEdit,onCancel,notify}) {
  const [receipt,setReceipt]=useState(null);
  const openReceipt=async order=>{
    try{setReceipt(await api(`/orders/my/${order.orderNumber}/receipt`))}
    catch(e){notify(e.message,'error')}
  };
  return <section className="panel">
    <div className="panel-head">
      <div><h3>Mis órdenes</h3><p>Consulta pagos y boletas. Solo las pendientes pueden editarse o cancelarse.</p></div>
    </div>
    <OrderTable orders={orders} actions={o=><>
      {o.receiptNumber&&<button className="icon neon" title="Ver boleta electrónica" onClick={()=>openReceipt(o)}><ReceiptText/></button>}
      {o.status==='PENDING'&&<>
        <button className="icon" title="Editar" onClick={()=>onEdit(o)}><Pencil/></button>
        <button className="icon danger" title="Cancelar" onClick={()=>onCancel(o)}><Trash2/></button>
      </>}
    </>}/>
    {receipt&&<ReceiptModal receipt={receipt} close={()=>setReceipt(null)}/>}
  </section>
}

function EditOrder({order,regions,close,done,notify}) { const [form,setForm]=useState({address:order.shippingAddress,region:order.shippingRegion,type:order.shippingType,lines:order.lines.map(l=>({sku:l.sku,productName:l.productName,quantity:l.quantity}))}); const submit=async()=>{try{await api(`/orders/my/${order.orderNumber}`,{method:'PUT',body:JSON.stringify({shippingAddress:form.address,shippingRegion:form.region,shippingType:form.type,lines:form.lines.map(l=>({sku:l.sku,quantity:Number(l.quantity)}))})});notify('Orden actualizada y stock recalculado.');done();}catch(e){notify(e.message,'error')}}; return <Modal title={`Editar ${order.orderNumber}`} close={close}><div className="form-grid"><label className="full">Dirección<input value={form.address} onChange={e=>setForm({...form,address:e.target.value})}/></label><label>Región<select value={form.region} onChange={e=>setForm({...form,region:e.target.value})}>{regions.map(r=><option key={r}>{r}</option>)}</select></label><label>Método<select value={form.type} onChange={e=>setForm({...form,type:e.target.value})}><option value="ECONOMICO">Económico</option><option value="MEJOR_RUTA">Mejor ruta</option><option value="EXPRESS">Express</option></select></label></div>{form.lines.map((l,i)=><div className="edit-line" key={l.sku}><span>{l.productName}</span><input type="number" min="1" value={l.quantity} onChange={e=>setForm({...form,lines:form.lines.map((x,j)=>j===i?{...x,quantity:e.target.value}:x)})}/></div>)}<button className="primary wide" onClick={submit}>Guardar cambios</button></Modal> }

function Profile({session,setSession,notify,logout}) { const [form,setForm]=useState({firstName:session.firstName,lastName:session.lastName,email:session.email,newPassword:''}); const save=async e=>{e.preventDefault();try{const data=await api('/auth/me',{method:'PUT',body:JSON.stringify({...form,newPassword:form.newPassword||null})});const next={...session,...data};localStorage.setItem('smartlogix_session',JSON.stringify(next));setSession(next);notify('Perfil actualizado. Por seguridad debes iniciar sesión nuevamente.');setTimeout(logout,1200);}catch(e){notify(e.message,'error')}}; return <section className="panel narrow"><div className="panel-head"><div><h3>Mi perfil</h3><p>Edita tus datos almacenados en la base de datos.</p></div></div><form className="form-grid" onSubmit={save}><label>Nombre<input required autoComplete="given-name" value={form.firstName} onChange={e=>setForm({...form,firstName:e.target.value})}/></label><label>Apellido<input required autoComplete="family-name" value={form.lastName} onChange={e=>setForm({...form,lastName:e.target.value})}/></label><label className="full">Correo<input required type="email" autoComplete="email" value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></label><label className="full">Nueva contraseña (opcional)<input type="password" autoComplete="new-password" minLength="6" value={form.newPassword} onChange={e=>setForm({...form,newPassword:e.target.value})}/></label><button className="primary">Guardar perfil</button></form></section> }

function Admin({session, logout, notify}) {
  const [tab, setTab] = useState('dashboard');
  const [inventory, setInventory] = useState([]);
  const [orders, setOrders] = useState([]);
  const [report, setReport] = useState(null);
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState({totalUsers: 0, clients: 0});
  const [product, setProduct] = useState(null);

  const getTotal = (order) => {
    return Number(
      order.total ||
      order.totalAmount ||
      order.finalTotal ||
      order.amount ||
      order.price ||
      0
    );
  };

  const getDate = (order) => {
    const value =
      order.createdAt ||
      order.created_at ||
      order.orderDate ||
      order.date ||
      new Date().toISOString();

    return String(value).substring(0, 10);
  };

  const getMonth = (order) => {
    return getDate(order).substring(0, 7);
  };

  const getRegion = (order) => {
    return (
      order.region ||
      order.shippingRegion ||
      order.destinationRegion ||
      order.destination ||
      order.shipmentRegion ||
      order.shipment?.region ||
      'Sin región'
    );
  };

  const getStatus = (order) => {
    return String(order.status || order.estado || 'PENDIENTE').toUpperCase();
  };

  const groupByOrders = (items, keyFn) => {
    const map = {};

    items.forEach((item) => {
      const key = keyFn(item);

      if (!map[key]) {
        map[key] = {
          name: key,
          label: key,
          fecha: key,
          dia: key,
          mes: key,
          region: key,
          estado: key,
          total: 0,
          count: 0,
          value: 0,
          cantidad: 0,
          orders: 0,
          ventas: 0,
          ingresos: 0
        };
      }

      const total = getTotal(item);

      map[key].count += 1;
      map[key].value += 1;
      map[key].cantidad += 1;
      map[key].orders += 1;
      map[key].ventas += 1;

      map[key].total += total;
      map[key].ingresos += total;
    });

    return Object.values(map);
  };

  const load = useCallback(async () => {
    try {
      const [i, o] = await Promise.all([
        api('/inventory/items'),
        api('/orders')
      ]);

      const safeInventory = Array.isArray(i) ? i : [];
      const safeOrders = Array.isArray(o) ? o : [];

      setInventory(safeInventory);
      setOrders(safeOrders);

      const totalRevenue = safeOrders.reduce((sum, order) => {
        return sum + getTotal(order);
      }, 0);

      const pendingOrders = safeOrders.filter((order) => {
        return getStatus(order).includes('PEND');
      }).length;

      const deliveredOrders = safeOrders.filter((order) => {
        const status = getStatus(order);
        return status.includes('ENTREG') || status.includes('DELIVER');
      }).length;

      const salesByDay = groupByOrders(safeOrders, getDate);
      const salesByMonth = groupByOrders(safeOrders, getMonth);
      const salesByRegion = groupByOrders(safeOrders, getRegion);
      const ordersByStatus = groupByOrders(safeOrders, getStatus);

      const totalUnits = safeInventory.reduce((sum, item) => {
        return sum + Number(
          item.stock ||
          item.quantity ||
          item.availableStock ||
          0
        );
      }, 0);

      const outOfStock = safeInventory.filter((item) => {
        return Number(item.stock || item.quantity || item.availableStock || 0) <= 0;
      }).length;

      const lowStock = safeInventory.filter((item) => {
        const stock = Number(item.stock || item.quantity || item.availableStock || 0);
        return stock > 0 && stock <= 5;
      }).length;

      const reportData = {
        totalOrders: safeOrders.length,
        ordersTotal: safeOrders.length,
        totalRevenue,
        revenue: totalRevenue,
        ingresos: totalRevenue,
        pendingOrders,
        pendientes: pendingOrders,
        deliveredOrders,
        entregadas: deliveredOrders,

        salesByDay,
        dailySales: salesByDay,
        ventasPorDia: salesByDay,
        ventasDia: salesByDay,

        salesByMonth,
        monthlySales: salesByMonth,
        ventasPorMes: salesByMonth,
        ventasMes: salesByMonth,

        salesByRegion,
        ventasPorRegion: salesByRegion,
        ventasRegion: salesByRegion,

        ordersByStatus,
        statusSummary: ordersByStatus,
        estadosOrdenes: ordersByStatus,
        ordersStatus: ordersByStatus
      };

      const statsData = {
        totalProducts: safeInventory.length,
        products: safeInventory.length,
        lowStock,
        stockBajo: lowStock,
        outOfStock,
        sinStock: outOfStock,
        totalUnits,
        availableUnits: totalUnits,
        unidadesDisponibles: totalUnits
      };

      setReport(reportData);
      setStats(statsData);

      setUsers({
        totalUsers: 0,
        clients: 0,
        clientes: 0
      });

    } catch (e) {
      notify(e.message, 'error');
    }
  }, [notify]);

  usePolling(load, 4000);

  const nav = [
    {id: 'dashboard', label: 'Dashboard', icon: ChartNoAxesCombined},
    {id: 'inventory', label: 'Inventario', icon: Boxes},
    {id: 'orders', label: 'Órdenes', icon: ClipboardList},
    {id: 'priceHistory', label: 'Búsquedas', icon: Search},
    {id: 'users', label: 'Usuarios', icon: Users}
  ];

  return (
    <Shell session={session} nav={nav} active={tab} setActive={setTab} logout={logout}>
      {tab === 'dashboard' && (
        <AdminDashboard report={report} stats={stats} users={users}/>
      )}

      {tab === 'inventory' && (
        <Inventory
          inventory={inventory}
          edit={setProduct}
          create={() => setProduct({...blankProduct})}
          refresh={load}
          notify={notify}
        />
      )}

      {tab === 'orders' && (
        <AdminOrders
          orders={orders}
          refresh={load}
          notify={notify}
        />
      )}

      {tab === 'priceHistory' && (
        <PriceHistory notify={notify}/>
      )}

      {tab === 'users' && (
        <UsersPage notify={notify}/>
      )}

      {product && (
        <ProductModal
          item={product}
          close={() => setProduct(null)}
          done={() => {
            setProduct(null);
            load();
          }}
          notify={notify}
        />
      )}
    </Shell>
  );
}

function AdminDashboard({report, stats, users}) {
  const formatCount = (value) => Number(value || 0);

  const normalizeChartData = (data) => {
    if (!data) return [];

    if (Array.isArray(data)) {
      return data.map((item) => ({
        name: item.name || item.label || item.estado || item.status || 'Sin dato',
        label: item.label || item.name || item.estado || item.status || 'Sin dato',
        value: Number(
          item.value ||
          item.count ||
          item.cantidad ||
          item.orders ||
          item.ventas ||
          item.total ||
          0
        )
      }));
    }

    if (typeof data === 'object') {
      return Object.entries(data).map(([name, value]) => ({
        name,
        label: name,
        value: typeof value === 'object'
          ? Number(value.value || value.count || value.cantidad || value.orders || value.total || 0)
          : Number(value || 0)
      }));
    }

    return [];
  };

  const salesByDay = normalizeChartData(
    report?.salesByDay ||
    report?.dailySales ||
    report?.ventasPorDia ||
    report?.ventasDia
  );

  const salesByRegion = normalizeChartData(
    report?.salesByRegion ||
    report?.ventasPorRegion ||
    report?.ventasRegion
  );

  const salesByMonth = normalizeChartData(
    report?.salesByMonth ||
    report?.monthlySales ||
    report?.ventasPorMes ||
    report?.ventasMes
  );

  const statusData = normalizeChartData(
    report?.ordersByStatus ||
    report?.statusSummary ||
    report?.estadosOrdenes ||
    report?.ordersStatus
  );

  return (
    <>
      <section className="stats">
        <Stat
          icon={ShoppingCart}
          label="Ventas"
          value={report?.salesCount || report?.totalOrders || 0}
        />

        <Stat
          icon={ClipboardList}
          label="Órdenes totales"
          value={report?.totalOrders || report?.ordersTotal || 0}
        />

        <Stat
          icon={Tag}
          label="Ingresos"
          value={money(report?.totalIncome || report?.totalRevenue || report?.ingresos || 0)}
        />

        <Stat
          icon={RefreshCw}
          label="Pendientes"
          value={report?.pendingOrders || report?.pendientes || 0}
        />

        <Stat
          icon={Package}
          label="Entregadas"
          value={report?.deliveredOrders || report?.entregadas || 0}
        />

        <Stat
          icon={Package}
          label="Sin stock"
          value={stats?.outOfStockProducts || stats?.outOfStock || stats?.sinStock || 0}
        />

        <Stat
          icon={Users}
          label="Clientes"
          value={users?.clients || users?.clientes || 0}
        />

        <Stat
          icon={Boxes}
          label="Unidades disponibles"
          value={stats?.availableUnits || stats?.unidadesDisponibles || stats?.totalUnits || 0}
        />
      </section>

      <section className="charts">
        <Chart title="Ventas por día">
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={salesByDay}>
              <CartesianGrid strokeDasharray="3 3"/>
              <XAxis dataKey="label"/>
              <YAxis/>
              <Tooltip formatter={(value) => formatCount(value)}/>
              <Line
                type="monotone"
                dataKey="value"
                stroke="#BC6FF1"
                dot={{fill: "#BC6FF1"}}
                strokeWidth={3}
              />
            </LineChart>
          </ResponsiveContainer>
        </Chart>

        <Chart title="Ventas por región">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={salesByRegion}>
              <CartesianGrid strokeDasharray="3 3"/>
              <XAxis dataKey="label" hide/>
              <YAxis/>
              <Tooltip formatter={(value) => formatCount(value)}/>
              <Bar dataKey="value" fill="#892CDC"/>
            </BarChart>
          </ResponsiveContainer>
        </Chart>

        <Chart title="Ventas por mes">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={salesByMonth}>
              <CartesianGrid strokeDasharray="3 3"/>
              <XAxis dataKey="label"/>
              <YAxis/>
              <Tooltip formatter={(value) => formatCount(value)}/>
              <Bar dataKey="value" fill="#892CDC"/>
            </BarChart>
          </ResponsiveContainer>
        </Chart>

        <Chart title="Estados de órdenes">
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie
                data={statusData}
                dataKey="value"
                nameKey="name"
                outerRadius={90}
                fill="#BC6FF1"
                label
              />
              <Tooltip formatter={(value) => formatCount(value)}/>
            </PieChart>
          </ResponsiveContainer>
        </Chart>
      </section>

      <section className="panel">
        <div className="panel-head">
          <div>
            <h3>Productos más vendidos</h3>
            <p>Calculado desde los detalles reales de órdenes.</p>
          </div>
        </div>

        <div className="ranking">
          {(report?.topProducts || []).map((p, i) => (
            <div key={p.sku || p.productName || i}>
              <em>{i + 1}</em>
              <span>
                <b>{p.productName || 'Producto'}</b>
                <small>{p.sku || 'Sin SKU'}</small>
              </span>
              <strong>{p.units || 0} unidades</strong>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}

function Inventory({inventory,edit,create,refresh,notify}) { const remove=async item=>{if(!confirm(`¿Eliminar ${item.productName}?`))return;try{await api(`/inventory/items/${item.sku}`,{method:'DELETE'});notify('Producto eliminado.');refresh();}catch(e){notify(e.message,'error')}}; const stock=async item=>{const q=prompt('Nuevo stock disponible',item.availableQuantity);if(q===null)return;try{await api(`/inventory/items/${item.sku}/stock`,{method:'PATCH',body:JSON.stringify({availableQuantity:Number(q)})});notify('Stock actualizado y visible en tienda.');refresh();}catch(e){notify(e.message,'error')}}; return <section className="panel"><div className="panel-head"><div><h3>Inventario</h3><p>CRUD completo y sincronizado con la tienda.</p></div><button className="primary" onClick={create}><Plus/>Agregar producto</button></div><div className="table-wrap"><table><thead><tr><th>Imagen</th><th>Producto</th><th>Categoría</th><th>Precio</th><th>Stock</th><th>Reservado</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{inventory.map(i=><tr key={i.sku}><td><img className="thumb" src={productImage(i.imageUrl)} alt={i.productName} onError={useDefaultImage}/></td><td><b>{i.productName}</b><small>{i.sku}</small></td><td>{i.category}</td><td>{money(i.price)}</td><td>{i.availableQuantity}</td><td>{i.reservedQuantity}</td><td><span className={`status ${i.active?'active':'inactive'}`}>{i.active?'Activo':'Inactivo'}</span></td><td className="actions"><button className="icon" onClick={()=>edit(i)} title="Editar"><Pencil/></button><button className="icon" onClick={()=>stock(i)} title="Modificar stock"><Boxes/></button><button className="icon danger" onClick={()=>remove(i)} title="Eliminar"><Trash2/></button></td></tr>)}</tbody></table></div>{inventory.length===0&&<Empty text="Inventario vacío. Agrega el primer producto desde este panel."/>}</section> }

function ProductModal({item,close,done,notify}) { const editing=Boolean(item.updatedAt); const [form,setForm]=useState(editing?{...item}:{...blankProduct}); const save=async e=>{e.preventDefault();try{if(editing){const {productName,category,warehouseCode,price,description,imageUrl,reorderLevel,active}=form;await api(`/inventory/items/${item.sku}`,{method:'PUT',body:JSON.stringify({productName,category,warehouseCode,price:Number(price),description,imageUrl,reorderLevel:Number(reorderLevel),active})});}else{await api('/inventory/items',{method:'POST',body:JSON.stringify({...form,price:Number(form.price),initialQuantity:Number(form.initialQuantity),reorderLevel:Number(form.reorderLevel)})});}notify(editing?'Producto actualizado.':'Producto creado.');done();}catch(e){notify(e.message,'error')}}; return <Modal title={editing?'Editar producto':'Agregar producto'} close={close}><form className="form-grid" onSubmit={save}><label>SKU<input required disabled={editing} value={form.sku} onChange={e=>setForm({...form,sku:e.target.value})}/></label><label>Nombre<input required value={form.productName} onChange={e=>setForm({...form,productName:e.target.value})}/></label><label>Categoría<input required value={form.category} onChange={e=>setForm({...form,category:e.target.value})}/></label><label>Precio<input required type="number" min="1" value={form.price} onChange={e=>setForm({...form,price:e.target.value})}/></label><label>Bodega<input required value={form.warehouseCode} onChange={e=>setForm({...form,warehouseCode:e.target.value})}/></label>{!editing&&<label>Stock inicial<input type="number" min="0" value={form.initialQuantity} onChange={e=>setForm({...form,initialQuantity:e.target.value})}/></label>}<label>Nivel reposición<input type="number" min="0" value={form.reorderLevel} onChange={e=>setForm({...form,reorderLevel:e.target.value})}/></label><label className="check"><input type="checkbox" checked={form.active} onChange={e=>setForm({...form,active:e.target.checked})}/>Producto activo</label><label className="full">URL imagen (opcional)<input placeholder="Se usará una imagen predeterminada" value={form.imageUrl} onChange={e=>setForm({...form,imageUrl:e.target.value})}/></label><label className="full">Descripción<textarea required value={form.description} onChange={e=>setForm({...form,description:e.target.value})}/></label><button className="primary">Guardar producto</button></form></Modal> }

function AdminOrders({orders,refresh,notify}) {
  const [receipt,setReceipt]=useState(null);
  const change=async(o,status)=>{
    try{
      await api(`/orders/${o.orderNumber}/status`,{method:'PATCH',body:JSON.stringify({status,reason:''})});
      notify('Estado actualizado y sincronizado con el cliente.');
      refresh();
    }catch(e){notify(e.message,'error')}
  };
  const openReceipt=async o=>{
    try{setReceipt(await api(`/orders/${o.orderNumber}/receipt`))}
    catch(e){notify(e.message,'error')}
  };
  return <section className="panel">
    <div className="panel-head"><div><h3>Gestión de órdenes</h3><p>Cambia estados y consulta pagos, boletas, inventario y envío.</p></div></div>
    <OrderTable orders={orders} showCustomer actions={o=><>
      {o.receiptNumber&&<button className="icon neon" title="Ver boleta" onClick={()=>openReceipt(o)}><ReceiptText/></button>}
      <select value={o.status} disabled={['DELIVERED','CANCELLED'].includes(o.status)} onChange={e=>change(o,e.target.value)}>
        <option value="PENDING">Pendiente</option>
        <option value="PREPARING">Preparando</option>
        <option value="SHIPPED">Enviado</option>
        <option value="DELIVERED">Entregado</option>
        <option value="CANCELLED">Cancelado</option>
      </select>
    </>}/>
    {receipt&&<ReceiptModal receipt={receipt} close={()=>setReceipt(null)}/>}
  </section>
}

function PriceHistory({notify}) {
  const [items,setItems]=useState([]);
  const load=useCallback(()=>api('/price-finder/history').then(setItems).catch(e=>notify(e.message,'error')),[notify]);
  useEffect(()=>{load()},[load]);
  return <section className="panel">
    <div className="panel-head"><div><h3>Historial de búsquedas reales</h3><p>Persistencia generada por SmartLogix Price Finder.</p></div><button className="secondary" onClick={load}>Actualizar</button></div>
    <div className="table-wrap"><table><thead><tr><th>Búsqueda</th><th>Mejor tienda</th><th>Producto</th><th>Precio</th><th>Resultados</th><th>Modo</th><th>Fecha</th></tr></thead><tbody>{items.map(x=><tr key={x.id}>
      <td><b>{x.query}</b></td><td>{x.bestStore}</td><td>{x.bestProduct}</td><td>{money(x.bestPrice)}</td><td>{x.resultsCount}</td><td>{x.sourceMode}</td><td>{date(x.searchedAt)}</td>
    </tr>)}</tbody></table></div>{items.length===0&&<Empty text="Aún no existen búsquedas registradas."/>}</section>
}

function UsersPage({notify}) { const [users,setUsers]=useState([]); const load=useCallback(()=>api('/auth/users').then(setUsers).catch(e=>notify(e.message,'error')),[notify]); useEffect(()=>{load()},[load]); return <section className="panel"><div className="panel-head"><div><h3>Usuarios registrados</h3><p>Cuentas reales almacenadas en auth-service.</p></div></div><div className="table-wrap"><table><thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Registro</th></tr></thead><tbody>{users.map(u=><tr key={u.id}><td>{u.firstName} {u.lastName}</td><td>{u.email}</td><td>{u.role}</td><td>{u.enabled?'Activo':'Bloqueado'}</td><td>{date(u.createdAt)}</td></tr>)}</tbody></table></div></section> }

function ReceiptModal({receipt,close}) {
  const printReceipt=()=>{
    const rows=(receipt.lines||[]).map(line=>`<tr><td>${escapeHtml(line.productName)}</td><td>${line.quantity}</td><td>${money(line.unitPrice)}</td><td>${money(line.lineTotal)}</td></tr>`).join('');
    const win=window.open('','_blank','width=900,height=700');
    if(!win)return;
    win.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>${receipt.receiptNumber}</title><style>
      body{font-family:Arial,sans-serif;color:#111;padding:36px;max-width:820px;margin:auto}
      header{display:flex;justify-content:space-between;border-bottom:3px solid #52057B;padding-bottom:18px}
      h1{margin:0;color:#52057B}.grid{display:grid;grid-template-columns:1fr 1fr;gap:8px 30px;margin:22px 0}
      table{width:100%;border-collapse:collapse}th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left}
      .totals{margin-left:auto;width:360px;margin-top:20px}.totals div{display:flex;justify-content:space-between;padding:7px}
      .total{font-size:20px;font-weight:bold;border-top:2px solid #52057B}.void{color:#b91c1c;font-weight:bold}
      footer{margin-top:30px;border-top:1px solid #ddd;padding-top:14px;font-size:12px;color:#666}
    </style></head><body>
      <header><div><h1>SmartLogix</h1><div>Boleta electrónica ficticia</div></div><div><b>${receipt.receiptNumber}</b><br>${date(receipt.issuedAt)}</div></header>
      ${receipt.voided?'<p class="void">DOCUMENTO ANULADO / PAGO REEMBOLSADO</p>':''}
      <div class="grid"><div><b>Orden:</b> ${receipt.orderNumber}</div><div><b>Cliente:</b> ${escapeHtml(receipt.customerName)}</div>
      <div><b>Correo:</b> ${escapeHtml(receipt.customerEmail)}</div><div><b>Región:</b> ${escapeHtml(receipt.shippingRegion)}</div>
      <div><b>Dirección:</b> ${escapeHtml(receipt.shippingAddress)}</div><div><b>Tarjeta:</b> ${receipt.cardBrand} ${receipt.maskedCard}</div>
      <div><b>Referencia:</b> ${receipt.paymentReference}</div><div><b>Autorización:</b> ${receipt.authorizationCode}</div></div>
      <table><thead><tr><th>Producto</th><th>Cant.</th><th>Precio</th><th>Total</th></tr></thead><tbody>${rows}</tbody></table>
      <div class="totals"><div><span>Subtotal</span><b>${money(receipt.subtotal)}</b></div><div><span>Descuento</span><b>-${money(receipt.discountAmount)}</b></div>
      <div><span>Envío</span><b>${money(receipt.shippingAmount)}</b></div><div><span>Neto</span><b>${money(receipt.netAmount)}</b></div>
      <div><span>IVA 19%</span><b>${money(receipt.taxAmount)}</b></div><div class="total"><span>Total pagado</span><b>${money(receipt.totalAmount)}</b></div></div>
      <footer>Código de verificación: ${receipt.verificationCode}. Documento electrónico ficticio generado para fines académicos.</footer>
      <script>window.onload=()=>window.print()</script>
    </body></html>`);
    win.document.close();
  };
  return <Modal title={`Boleta ${receipt.receiptNumber}`} close={close}>
    <article className={`receipt ${receipt.voided?'voided':''}`}>
      <header className="receipt-head"><div><div className="logo-mark small">SL</div><div><h2>SmartLogix</h2><p>Boleta electrónica ficticia</p></div></div><div><strong>{receipt.receiptNumber}</strong><span>{date(receipt.issuedAt)}</span></div></header>
      {receipt.voided&&<div className="receipt-warning">Documento anulado · pago reembolsado</div>}
      <div className="receipt-info">
        <div><span>Orden</span><b>{receipt.orderNumber}</b></div>
        <div><span>Cliente</span><b>{receipt.customerName}</b></div>
        <div><span>Correo</span><b>{receipt.customerEmail}</b></div>
        <div><span>Región</span><b>{receipt.shippingRegion}</b></div>
        <div className="wide-info"><span>Dirección</span><b>{receipt.shippingAddress}</b></div>
        <div><span>Pago</span><b>{receipt.cardBrand} {receipt.maskedCard}</b></div>
        <div><span>Estado</span><b>{receipt.paymentStatus}</b></div>
      </div>
      <div className="table-wrap receipt-table"><table><thead><tr><th>Producto</th><th>Cantidad</th><th>Precio</th><th>Total</th></tr></thead><tbody>
        {(receipt.lines||[]).map(line=><tr key={line.sku}><td>{line.productName}<small>{line.sku}</small></td><td>{line.quantity}</td><td>{money(line.unitPrice)}</td><td>{money(line.lineTotal)}</td></tr>)}
      </tbody></table></div>
      <div className="receipt-totals">
        <div><span>Subtotal</span><b>{money(receipt.subtotal)}</b></div>
        <div><span>Descuento</span><b>-{money(receipt.discountAmount)}</b></div>
        <div><span>Envío</span><b>{money(receipt.shippingAmount)}</b></div>
        <div><span>Neto</span><b>{money(receipt.netAmount)}</b></div>
        <div><span>IVA 19%</span><b>{money(receipt.taxAmount)}</b></div>
        <div className="grand-total"><span>Total pagado</span><b>{money(receipt.totalAmount)}</b></div>
      </div>
      <footer className="receipt-footer">Verificación: {receipt.verificationCode} · Referencia: {receipt.paymentReference}</footer>
    </article>
    <button className="primary wide" onClick={printReceipt}><Printer/>Imprimir / guardar como PDF</button>
  </Modal>
}

function OrderTable({orders=[],actions,showCustomer=false}) { return <div className="table-wrap"><table><thead><tr><th>Número</th>{showCustomer&&<th>Cliente</th>}<th>Fecha</th><th>Estado</th><th>Productos</th><th>Envío</th><th>Total</th>{actions&&<th>Acciones</th>}</tr></thead><tbody>{orders.map(o=><tr key={o.orderNumber}><td><b>{o.orderNumber}</b><small>{o.trackingCode}</small></td>{showCustomer&&<td>{o.customerName}<small>{o.customerEmail}</small></td>}<td>{date(o.createdAt)}</td><td><span className={`status ${o.status.toLowerCase()}`}>{statusLabel(o.status)}</span></td><td>{o.lines?.map(l=><span className="line" key={l.sku}>{l.productName} × {l.quantity}</span>)}</td><td>{routeLabel(o.shippingType)}<small>{o.shippingCarrier}</small>{o.shippingRouteCode&&<small>{o.shippingRouteName} · {o.shippingRouteCode} · {o.shippingEstimatedDays} días</small>}</td><td><b>{money(o.totalAmount)}</b>{o.discountApplied&&<small>10% aplicado</small>}</td>{actions&&<td className="actions">{actions(o)}</td>}</tr>)}</tbody></table>{orders.length===0&&<Empty text="No hay órdenes registradas."/>}</div> }
function Stat({icon:Icon,label,value}) { return <article className="stat"><div><Icon/></div><span>{label}</span><strong>{value}</strong></article> }
function Chart({title,children}) { return <section className="panel chart"><h3>{title}</h3>{children}</section> }
function Modal({title,close,children}) { return <div className="modal-backdrop" onMouseDown={e=>e.target===e.currentTarget&&close()}><section className="modal"><header><h2>{title}</h2><button className="icon" onClick={close}><X/></button></header>{children}</section></div> }
function Empty({text}) { return <div className="empty"><Package/><p>{text}</p></div> }
const routeLabel=t=>({ECONOMICO:'Envío Económico',MEJOR_RUTA:'Mejor Ruta',EXPRESS:'Entrega Express'})[t]||t;
const escapeHtml=value=>String(value??'').replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
const statusLabel=s=>({PENDING:'Pendiente',PREPARING:'Preparando',SHIPPED:'Enviado',DELIVERED:'Entregado',CANCELLED:'Cancelado',REJECTED:'Rechazado',FAILED:'Fallido'})[s]||s;
