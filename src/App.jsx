import { useEffect, useMemo, useRef, useState } from "react";
import {
  Bot,
  BrainCircuit,
  ChevronDown,
  CircleHelp,
  Database,
  Download,
  FileJson,
  GripVertical,
  Image,
  Layers3,
  MessageSquareText,
  MoreHorizontal,
  Play,
  Plus,
  Rocket,
  Save,
  Search,
  Settings2,
  Sparkles,
  TerminalSquare,
  Trash2,
  Upload,
  Webhook,
  Workflow,
  X,
  Zap,
} from "lucide-react";

const PALETTE = [
  {
    title: "Eingaben",
    items: [
      { type: "prompt", label: "Benutzer-Prompt", icon: MessageSquareText, color: "violet", hint: "Text entgegennehmen" },
      { type: "file", label: "Datei-Upload", icon: Upload, color: "blue", hint: "PDF, Bild oder Text" },
      { type: "webhook", label: "Webhook", icon: Webhook, color: "orange", hint: "Externe Daten" },
    ],
  },
  {
    title: "Intelligenz",
    items: [
      { type: "model", label: "KI-Modell", icon: BrainCircuit, color: "pink", hint: "LLM auswählen" },
      { type: "vision", label: "Bildanalyse", icon: Image, color: "cyan", hint: "Bilder verstehen" },
      { type: "agent", label: "KI-Agent", icon: Bot, color: "lime", hint: "Autonome Aufgaben" },
    ],
  },
  {
    title: "Wissen & Logik",
    items: [
      { type: "knowledge", label: "Wissensbasis", icon: Database, color: "yellow", hint: "Eigene Daten nutzen" },
      { type: "condition", label: "Bedingung", icon: Workflow, color: "purple", hint: "Wenn / Sonst" },
      { type: "code", label: "Code-Block", icon: TerminalSquare, color: "slate", hint: "Eigene Logik" },
    ],
  },
];

const TEMPLATES = [
  {
    name: "Support-Assistent",
    description: "Beantwortet Fragen mit deinem Firmenwissen.",
    icon: MessageSquareText,
    nodes: ["prompt", "knowledge", "model"],
  },
  {
    name: "Dokument-Analyst",
    description: "Liest Dateien und erstellt strukturierte Zusammenfassungen.",
    icon: FileJson,
    nodes: ["file", "model"],
  },
  {
    name: "Autonomer Rechercheur",
    description: "Plant und bearbeitet komplexe Rechercheaufgaben.",
    icon: Bot,
    nodes: ["prompt", "agent", "model"],
  },
];

const INITIAL_NODES = [
  { id: "n1", type: "prompt", label: "Frage des Nutzers", x: 70, y: 155, description: "Worüber möchtest du mehr erfahren?" },
  { id: "n2", type: "knowledge", label: "Produktwissen", x: 345, y: 155, description: "12 Dokumente · 4,8 MB" },
  { id: "n3", type: "model", label: "Antwort generieren", x: 620, y: 155, description: "GPT-4.1 mini · Kreativ" },
];

const allItems = PALETTE.flatMap((section) => section.items);
const getMeta = (type) => allItems.find((item) => item.type === type) || allItems[0];

function App() {
  const [nodes, setNodes] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("neural-forge-workflow")) || INITIAL_NODES;
    } catch {
      return INITIAL_NODES;
    }
  });
  const [selectedId, setSelectedId] = useState("n3");
  const [query, setQuery] = useState("");
  const [projectName, setProjectName] = useState("Mein KI-Assistent");
  const [saved, setSaved] = useState(true);
  const [showTemplates, setShowTemplates] = useState(false);
  const [showRun, setShowRun] = useState(false);
  const [runState, setRunState] = useState("idle");
  const [testPrompt, setTestPrompt] = useState("Welche Vorteile bietet unser Pro-Tarif?");
  const [toast, setToast] = useState("");
  const canvasRef = useRef(null);
  const dragRef = useRef(null);

  const selected = nodes.find((node) => node.id === selectedId);
  const visiblePalette = useMemo(
    () =>
      PALETTE.map((section) => ({
        ...section,
        items: section.items.filter((item) =>
          `${item.label} ${item.hint}`.toLowerCase().includes(query.toLowerCase()),
        ),
      })).filter((section) => section.items.length),
    [query],
  );

  useEffect(() => {
    setSaved(false);
    const timer = setTimeout(() => {
      localStorage.setItem("neural-forge-workflow", JSON.stringify(nodes));
      setSaved(true);
    }, 500);
    return () => clearTimeout(timer);
  }, [nodes]);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(""), 2400);
    return () => clearTimeout(timer);
  }, [toast]);

  const addNode = (type) => {
    const meta = getMeta(type);
    const offset = nodes.length * 18;
    const node = {
      id: `n${Date.now()}`,
      type,
      label: meta.label,
      description: meta.hint,
      x: 110 + (offset % 430),
      y: 95 + (offset % 260),
    };
    setNodes((current) => [...current, node]);
    setSelectedId(node.id);
  };

  const updateSelected = (field, value) => {
    setNodes((current) => current.map((node) => (node.id === selectedId ? { ...node, [field]: value } : node)));
  };

  const removeSelected = () => {
    setNodes((current) => current.filter((node) => node.id !== selectedId));
    setSelectedId(null);
  };

  const onPointerDown = (event, node) => {
    if (event.button !== 0) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    dragRef.current = { id: node.id, startX: event.clientX, startY: event.clientY, nodeX: node.x, nodeY: node.y };
    setSelectedId(node.id);
  };

  const onPointerMove = (event) => {
    const drag = dragRef.current;
    if (!drag) return;
    const bounds = canvasRef.current.getBoundingClientRect();
    const x = Math.max(16, Math.min(bounds.width - 232, drag.nodeX + event.clientX - drag.startX));
    const y = Math.max(16, Math.min(bounds.height - 105, drag.nodeY + event.clientY - drag.startY));
    setNodes((current) => current.map((node) => (node.id === drag.id ? { ...node, x, y } : node)));
  };

  const applyTemplate = (template) => {
    const templateNodes = template.nodes.map((type, index) => {
      const meta = getMeta(type);
      return {
        id: `n${Date.now()}-${index}`,
        type,
        label: meta.label,
        description: meta.hint,
        x: 70 + index * 275,
        y: 155,
      };
    });
    setNodes(templateNodes);
    setProjectName(template.name);
    setSelectedId(templateNodes[0].id);
    setShowTemplates(false);
    setToast(`Vorlage „${template.name}“ geladen`);
  };

  const runWorkflow = () => {
    setShowRun(true);
    setRunState("running");
    setTimeout(() => setRunState("done"), 1400);
  };

  const exportProject = () => {
    const blob = new Blob([JSON.stringify({ name: projectName, version: 1, nodes }, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${projectName.toLowerCase().replace(/\s+/g, "-")}.json`;
    link.click();
    URL.revokeObjectURL(url);
    setToast("Projekt wurde exportiert");
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-mark"><Sparkles size={18} /></div>
          <span>NEURAL<span>FORGE</span></span>
        </div>
        <div className="project-title">
          <input value={projectName} onChange={(event) => setProjectName(event.target.value)} aria-label="Projektname" />
          <span className={`save-status ${saved ? "saved" : ""}`}>{saved ? "Gespeichert" : "Speichert …"}</span>
        </div>
        <div className="top-actions">
          <button className="icon-button" title="Hilfe"><CircleHelp size={18} /></button>
          <button className="button ghost" onClick={exportProject}><Download size={16} /> Exportieren</button>
          <button
            className="button primary"
            onClick={() => {
              setShowRun(true);
              setRunState("idle");
            }}
          >
            <Play size={16} fill="currentColor" /> Testen
          </button>
        </div>
      </header>

      <main className="workspace">
        <aside className="sidebar">
          <div className="sidebar-head">
            <span>Bausteine</span>
            <button className="icon-button small"><MoreHorizontal size={18} /></button>
          </div>
          <label className="search">
            <Search size={15} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Baustein suchen …" />
          </label>
          <div className="palette">
            {visiblePalette.map((section) => (
              <section key={section.title}>
                <h3>{section.title}</h3>
                {section.items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <button className="palette-item" key={item.type} onClick={() => addNode(item.type)}>
                      <span className={`node-icon ${item.color}`}><Icon size={17} /></span>
                      <span><strong>{item.label}</strong><small>{item.hint}</small></span>
                      <Plus size={15} className="add-icon" />
                    </button>
                  );
                })}
              </section>
            ))}
          </div>
          <button className="template-button" onClick={() => setShowTemplates(true)}>
            <Layers3 size={17} /><span><strong>Vorlagen entdecken</strong><small>Schneller starten</small></span>
          </button>
        </aside>

        <section className="canvas-wrap">
          <div className="canvas-toolbar">
            <div className="crumb"><Workflow size={15} /> Workflow <span>/</span> Hauptablauf</div>
            <div className="toolbar-actions">
              <button title="Speichern" onClick={() => setToast("Projekt ist gespeichert")}><Save size={16} /></button>
              <span />
              <button className="zoom">100% <ChevronDown size={13} /></button>
            </div>
          </div>
          <div
            className="canvas"
            ref={canvasRef}
            onPointerMove={onPointerMove}
            onPointerUp={() => { dragRef.current = null; }}
            onPointerCancel={() => { dragRef.current = null; }}
          >
            <svg className="connections" width="100%" height="100%">
              {nodes.slice(0, -1).map((node, index) => {
                const next = nodes[index + 1];
                return (
                  <path
                    key={`${node.id}-${next.id}`}
                    d={`M ${node.x + 216} ${node.y + 45} C ${node.x + 246} ${node.y + 45}, ${next.x - 30} ${next.y + 45}, ${next.x} ${next.y + 45}`}
                  />
                );
              })}
            </svg>
            <div className="canvas-label"><span><Zap size={13} fill="currentColor" /></span> Von links nach rechts verbinden</div>
            {nodes.map((node, index) => {
              const meta = getMeta(node.type);
              const Icon = meta.icon;
              return (
                <article
                  key={node.id}
                  className={`flow-node ${selectedId === node.id ? "selected" : ""}`}
                  style={{ transform: `translate(${node.x}px, ${node.y}px)` }}
                  onPointerDown={(event) => onPointerDown(event, node)}
                >
                  <span className="port input-port" />
                  <div className="node-grab"><GripVertical size={14} /><span>0{index + 1}</span></div>
                  <div className="node-main">
                    <span className={`node-icon ${meta.color}`}><Icon size={18} /></span>
                    <div><strong>{node.label}</strong><small>{node.description}</small></div>
                  </div>
                  <span className="port output-port" />
                </article>
              );
            })}
            {!nodes.length && (
              <div className="empty-canvas">
                <div><Plus size={24} /></div>
                <h2>Dein Workflow ist noch leer</h2>
                <p>Wähle links einen Baustein oder starte mit einer Vorlage.</p>
              </div>
            )}
            <div className="minimap">
              <div className="mini-flow">{nodes.slice(0, 4).map((node) => <i key={node.id} />)}</div>
            </div>
          </div>
        </section>

        <aside className={`inspector ${selected ? "" : "empty"}`}>
          {selected ? (
            <>
              <div className="inspector-head">
                <div><span>Eigenschaften</span><small>Baustein konfigurieren</small></div>
                <button className="icon-button small" onClick={() => setSelectedId(null)}><X size={17} /></button>
              </div>
              <div className="inspector-body">
                <label className="field"><span>Name</span><input value={selected.label} onChange={(e) => updateSelected("label", e.target.value)} /></label>
                <label className="field"><span>Beschreibung</span><textarea value={selected.description} onChange={(e) => updateSelected("description", e.target.value)} rows="3" /></label>
                {selected.type === "model" && (
                  <>
                    <label className="field"><span>Modell</span><select><option>GPT-4.1 mini</option><option>Claude Sonnet</option><option>Gemini Flash</option><option>Lokales Modell</option></select></label>
                    <label className="field"><span>Kreativität <b>0.7</b></span><input type="range" min="0" max="1" step="0.1" defaultValue="0.7" /></label>
                  </>
                )}
                <div className="settings-card">
                  <div><Settings2 size={16} /><span><strong>Erweiterte Optionen</strong><small>Ausgaben, Fehler & Limits</small></span></div>
                  <ChevronDown size={15} />
                </div>
                <div className="node-info"><span>Typ</span><code>{selected.type}</code><span>ID</span><code>{selected.id.slice(-8)}</code></div>
              </div>
              <div className="inspector-footer"><button className="danger-button" onClick={removeSelected}><Trash2 size={16} /> Baustein löschen</button></div>
            </>
          ) : (
            <div className="nothing-selected"><Settings2 size={24} /><strong>Nichts ausgewählt</strong><p>Wähle einen Baustein, um seine Eigenschaften zu bearbeiten.</p></div>
          )}
        </aside>
      </main>

      {showTemplates && (
        <div className="modal-backdrop" onMouseDown={() => setShowTemplates(false)}>
          <div className="modal" onMouseDown={(event) => event.stopPropagation()}>
            <div className="modal-head"><div><span className="eyebrow">SCHNELLSTART</span><h2>Was möchtest du bauen?</h2><p>Wähle eine Vorlage und passe sie frei an.</p></div><button className="icon-button" onClick={() => setShowTemplates(false)}><X size={19} /></button></div>
            <div className="template-grid">
              {TEMPLATES.map((template) => {
                const Icon = template.icon;
                return (
                  <button key={template.name} onClick={() => applyTemplate(template)}>
                    <span className="template-icon"><Icon size={21} /></span><strong>{template.name}</strong><p>{template.description}</p><small>{template.nodes.length} Bausteine <span>→</span></small>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {showRun && (
        <div className="run-panel">
          <div className="run-head"><div><span className="live-dot" /> TESTLABOR</div><button className="icon-button" onClick={() => setShowRun(false)}><X size={18} /></button></div>
          <div className="run-content">
            <h2>Workflow ausprobieren</h2><p>Teste deine KI, bevor du sie veröffentlichst.</p>
            <label className="field"><span>Test-Eingabe</span><textarea value={testPrompt} onChange={(e) => setTestPrompt(e.target.value)} rows="4" /></label>
            <button className="button primary wide" onClick={runWorkflow} disabled={runState === "running"}>
              {runState === "running" ? <><span className="spinner" /> Workflow läuft …</> : <><Play size={16} fill="currentColor" /> Ausführen</>}
            </button>
            {runState === "done" && (
              <div className="result-card"><div><Sparkles size={16} /><strong>Antwort</strong><span>1,2 s</span></div><p>Der Pro-Tarif bietet erweiterte KI-Modelle, höhere Nutzungslimits und die Möglichkeit, eigene Wissensquellen einzubinden. Außerdem erhältst du priorisierten Support.</p><small>3 Bausteine erfolgreich ausgeführt · 284 Tokens</small></div>
            )}
          </div>
          <div className="deploy-card"><Rocket size={18} /><div><strong>Bereit für echte Nutzer?</strong><small>Veröffentliche deinen Workflow als API oder Web-App.</small></div><button>Veröffentlichen</button></div>
        </div>
      )}
      {toast && <div className="toast"><span>✓</span>{toast}</div>}
    </div>
  );
}

export default App;
