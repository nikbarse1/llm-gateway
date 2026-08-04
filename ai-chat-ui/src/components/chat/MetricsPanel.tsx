import { useState, type ReactNode } from 'react';
import { type OptimizationMetrics } from '../../types/api';
import { TrendingDown, Cpu, Zap, ChevronDown, ChevronUp } from 'lucide-react';

interface MetricsPanelProps {
  metrics: OptimizationMetrics | null;
  wasOptimized: boolean;
  isVisible: boolean;
}

export function MetricsPanel({ metrics, wasOptimized, isVisible }: MetricsPanelProps) {
  const [expanded, setExpanded] = useState(false);

  if (!isVisible || !metrics) return null;

  const usage = metrics.usageMetrics;
  const routing = metrics.routingDecision;
  const payload = metrics.payloadSnapshot;

  const savingsColor =
    usage.savingsPercentage > 0 ? 'text-emerald-400' : 'text-zinc-400';

  return (
    <div className="py-2 px-4 md:px-8 w-full">
      <div className="max-w-3xl mx-auto rounded-lg border border-emerald-900/30 bg-emerald-950/20 p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <TrendingDown size={18} className={savingsColor} />
            <span className="text-sm font-semibold text-zinc-200">Token Optimization Proof</span>
          </div>
          <button
            onClick={() => setExpanded((prev) => !prev)}
            className="text-xs text-emerald-400 hover:text-emerald-300 flex items-center gap-1"
          >
            {expanded ? 'Less' : 'More'}
            {expanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
          </button>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
          <MetricTile
            icon={<Zap size={14} />}
            label="Status"
            value={wasOptimized ? 'Yes' : 'No'}
            color={wasOptimized ? 'text-emerald-400' : 'text-zinc-400'}
          />
          <MetricTile
            icon={<Cpu size={14} />}
            label="Executed Provider"
            value={routing?.executedProvider ?? 'N/A'}
          />
          <MetricTile
            label="Unoptimized"
            value={usage.unoptimizedPromptTokens.toLocaleString()}
          />
          <MetricTile
            label="Optimized"
            value={usage.optimizedPromptTokens.toLocaleString()}
          />
          <MetricTile
            label="Tokens Saved"
            value={usage.tokensSaved.toLocaleString()}
            color={usage.tokensSaved > 0 ? 'text-emerald-400' : 'text-zinc-400'}
          />
          <MetricTile
            label="Savings %"
            value={`${usage.savingsPercentage.toFixed(2)}%`}
            color={savingsColor}
          />
          <MetricTile
            label="Context Window"
            value={payload?.contextWindowSize.toLocaleString() ?? 'N/A'}
          />
          <MetricTile
            label="Headroom"
            value={payload?.remainingHeadroom.toLocaleString() ?? 'N/A'}
          />
        </div>

        {expanded && metrics.historyOptimization && (
          <div className="mt-3 pt-3 border-t border-emerald-900/30">
            <h4 className="text-xs font-semibold text-zinc-400 mb-2">History Optimization</h4>
            <div className="grid grid-cols-3 gap-3 text-sm">
              <MetricTile
                label="Raw History"
                value={metrics.historyOptimization.rawHistoryTokens.toLocaleString()}
              />
              <MetricTile
                label="Summarized"
                value={metrics.historyOptimization.summarizedHistoryTokens.toLocaleString()}
              />
              <MetricTile
                label="History Saved"
                value={metrics.historyOptimization.historyTokensSaved.toLocaleString()}
                color={metrics.historyOptimization.historyTokensSaved > 0 ? 'text-emerald-400' : 'text-zinc-400'}
              />
            </div>
          </div>
        )}

        {expanded && metrics.budgetAllocation && (
          <div className="mt-3 pt-3 border-t border-emerald-900/30">
            <h4 className="text-xs font-semibold text-zinc-400 mb-2">Budget Actions</h4>
            {metrics.budgetAllocation.actions.length === 0 ? (
              <p className="text-xs text-zinc-500">No budget actions were needed.</p>
            ) : (
              <ul className="space-y-1">
                {metrics.budgetAllocation.actions.map((action, idx) => (
                  <li
                    key={idx}
                    className="text-xs text-zinc-400 flex items-center justify-between"
                  >
                    <span className="font-medium text-zinc-300">{action.section}</span>
                    <span>{action.actionTaken} — {action.requestedTokens} → {action.allocatedTokens}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function MetricTile({
  icon,
  label,
  value,
  color = 'text-zinc-200',
}: {
  icon?: ReactNode;
  label: string;
  value: string;
  color?: string;
}) {
  return (
    <div className="bg-zinc-900/60 rounded p-2">
      <div className="text-zinc-500 text-[10px] uppercase tracking-wider flex items-center gap-1">
        {icon}
        {label}
      </div>
      <div className={`font-mono font-semibold mt-1 ${color}`}>{value}</div>
    </div>
  );
}
